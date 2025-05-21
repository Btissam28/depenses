package com.example.budget;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FinancialHealthCalculator {

    public interface ScoreCalculationCallback {
        void onScoreCalculated(int score, String level, int colorResId);
        void onCalculationFailed(DatabaseError error);
    }

    public static void calculateFinancialHealth(DatabaseReference userRef, ScoreCalculationCallback callback) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Récupérer les données nécessaires
                Double initialBalance = getDoubleValue(snapshot, "soldeInitial");
                Double currentBalance = getDoubleValue(snapshot, "solde");

                // 2. Calculer les totaux
                double totalIncome = calculateTotal(snapshot, "incomes");
                double totalExpenses = calculateTotal(snapshot, "expenses");

                // 3. Récupérer les budgets
                Map<String, Double> budgets = new HashMap<>();
                DataSnapshot budgetsSnapshot = snapshot.child("budgets");
                if (budgetsSnapshot.exists()) {
                    for (DataSnapshot budget : budgetsSnapshot.getChildren()) {
                        budgets.put(budget.getKey(), budget.getValue(Double.class));
                    }
                }

                // 4. Calculer le score (0-100)
                int score = calculateScore(initialBalance, totalIncome, totalExpenses, budgets);

                // 5. Déterminer le niveau
                ScoreLevel level = determineScoreLevel(score);

                // 6. Retourner le résultat
                if (callback != null) {
                    callback.onScoreCalculated(score, level.label, level.colorResId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onCalculationFailed(error);
                }
            }
        });
    }

    private static double calculateTotal(DataSnapshot snapshot, String childPath) {
        double total = 0;
        DataSnapshot childSnapshot = snapshot.child(childPath);
        if (childSnapshot.exists()) {
            for (DataSnapshot item : childSnapshot.getChildren()) {
                Double amount = item.child("montant").getValue(Double.class);
                if (amount != null) {
                    total += amount;
                }
            }
        }
        return total;
    }

    private static Double getDoubleValue(DataSnapshot snapshot, String path) {
        DataSnapshot child = snapshot.child(path);
        return child.exists() ? child.getValue(Double.class) : 0.0;
    }

    private static int calculateScore(Double initialBalance, double totalIncome,
                                      double totalExpenses, Map<String, Double> budgets) {
        int score = 100;
        double totalBudget = initialBalance + totalIncome;

        // 1. Ratio dépenses/revenus (50 points)
        if (totalBudget > 0) {
            double expenseRatio = (totalExpenses / totalBudget) * 100;
            if (expenseRatio > 90) score -= 25;
            else if (expenseRatio > 80) score -= 15;
            else if (expenseRatio > 70) score -= 5;
        }

        // 2. Respect des budgets par catégorie (30 points)
        int exceededCategories = 0;
        for (Map.Entry<String, Double> entry : budgets.entrySet()) {
            if (entry.getValue() > 0 && totalExpenses > entry.getValue()) {
                exceededCategories++;
            }
        }
        score -= Math.min(exceededCategories * 5, 30);

        // 3. Diversification des dépenses (20 points)
        // (Cette partie nécessiterait plus de données sur les catégories de dépenses)

        return Math.max(0, Math.min(100, score));
    }

    private static ScoreLevel determineScoreLevel(int score) {
        if (score >= 75) {
            return new ScoreLevel("Bonne gestion", R.color.green_4ade80);
        } else if (score >= 50) {
            return new ScoreLevel("À surveiller", R.color.yellow_facc15);
        } else {
            return new ScoreLevel("Attention", R.color.red_f87171);
        }
    }

    private static class ScoreLevel {
        String label;
        int colorResId;

        ScoreLevel(String label, int colorResId) {
            this.label = label;
            this.colorResId = colorResId;
        }
    }
}