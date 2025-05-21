package com.example.budget;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private SimpleDateFormat dateFormat;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Vérifier si l'utilisateur est connecté
        if (currentUser == null) {
            startActivity(new Intent(this, Connexion.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialiser le format de date
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Initialiser le DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Configurer le solde
        setupUserBalance();

        // Bouton menu
        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Menu navigation
        setupNavigationMenu(currentUser);

        // Actions principales
        setupMainActions();

        // Configurer le bouton d'historique
        setupHistoryButton();

        // Configurer les catégories par défaut
        setupDefaultCategories();

        // Configurer le graphique des dépenses
        setupExpensesChart();
        // Configurer la barre de progression du budget
        setupBudgetProgress();
        calculateFinancialHealthScore();
        updateScoreDate();
        loadRecentTransactions();
    }

    private void setupUserBalance() {
        TextView soldeText = findViewById(R.id.soldeTotalText);
        userRef.child("solde").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double solde = snapshot.getValue(Double.class);
                if (solde != null) {
                    soldeText.setText(String.format(Locale.getDefault(), "%.2f", solde));
                } else {
                    soldeText.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur de lecture du solde", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigationMenu(FirebaseUser currentUser) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Déjà sur l'accueil
            } else if (id == R.id.nav_transactions) {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, StatisticsActivity.class));

            } else if (id == R.id.nav_profile) {

            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, Connexion.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupMainActions() {
        // Dépense
        LinearLayout expenseAction = findViewById(R.id.expenseAction);
        expenseAction.setOnClickListener(v -> showExpenseDialog());

        // Revenu
        LinearLayout incomeAction = findViewById(R.id.incomeAction);
        incomeAction.setOnClickListener(v -> showIncomeDialog());

        // Budget
        LinearLayout budgetAction = findViewById(R.id.gestion_budget);
        budgetAction.setOnClickListener(v -> {
            Intent intent = new Intent(this, BudgetActivity.class);
            startActivity(intent);
        });

        // Ajout de solde (ancienne version)
        AppCompatButton budgetActionButton = findViewById(R.id.budgetAction);
        if (budgetActionButton != null) {
            budgetActionButton.setOnClickListener(v -> showBudgetDialog());
        }
    }

    private void showExpenseDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_expense, null);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();

        EditText amount = sheetView.findViewById(R.id.expenseAmount);
        Spinner category = sheetView.findViewById(R.id.expenseCategory);
        EditText date = sheetView.findViewById(R.id.expenseDate);
        EditText description = sheetView.findViewById(R.id.expenseDescription);
        Button save = sheetView.findViewById(R.id.saveExpense);

        // Date actuelle
        Calendar calendar = Calendar.getInstance();
        date.setText(dateFormat.format(calendar.getTime()));

        // Sélecteur de date
        date.setOnClickListener(v -> new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    date.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show());

        // Charger les catégories
        loadCategoriesIntoSpinner(category);

        save.setOnClickListener(v -> {
            String amountStr = amount.getText().toString();
            String categoryStr = category.getSelectedItem() != null ?
                    category.getSelectedItem().toString() : "";
            String dateStr = date.getText().toString();
            String descStr = description.getText().toString();

            if (amountStr.isEmpty()) {
                amount.setError("Montant requis");
                return;
            }

            if (categoryStr.isEmpty()) {
                Toast.makeText(this, "Catégorie requise", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double montant = Double.parseDouble(amountStr);
                Date expenseDate = dateFormat.parse(dateStr);

                // Vérifier le solde
                userRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double solde = snapshot.getValue(Double.class);
                        if (solde == null) solde = 0.0;

                        if (montant > solde) {
                            Toast.makeText(MainActivity.this, "Solde insuffisant", Toast.LENGTH_SHORT).show();
                        } else {
                            // Mettre à jour le solde
                            userRef.child("solde").setValue(solde - montant);

                            // Enregistrer la dépense
                            DatabaseReference expensesRef = userRef.child("expenses").push();
                            Expenses expense = new Expenses(montant, categoryStr, expenseDate, descStr);
                            expensesRef.setValue(expense)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(MainActivity.this, "Dépense enregistrée", Toast.LENGTH_SHORT).show();
                                        bottomSheetDialog.dismiss();
                                        calculateFinancialHealthScore();
                                        loadRecentTransactions(); // Rafraîchir les transactions
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(MainActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Erreur de lecture du solde", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (NumberFormatException e) {
                amount.setError("Montant invalide");
            } catch (ParseException e) {
                date.setError("Date invalide");
            }
        });
    }

    private void showIncomeDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_income, null);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();

        EditText amount = sheetView.findViewById(R.id.incomeAmount);
        EditText date = sheetView.findViewById(R.id.incomeDate);
        EditText description = sheetView.findViewById(R.id.incomeDescription);
        Button save = sheetView.findViewById(R.id.saveIncome);

        // Date actuelle
        Calendar calendar = Calendar.getInstance();
        date.setText(dateFormat.format(calendar.getTime()));

        // Sélecteur de date
        date.setOnClickListener(v -> new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    date.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show());

        save.setOnClickListener(v -> {
            String amountStr = amount.getText().toString();
            String dateStr = date.getText().toString();
            String descStr = description.getText().toString();

            if (amountStr.isEmpty()) {
                amount.setError("Montant requis");
                return;
            }

            try {
                double montant = Double.parseDouble(amountStr);
                Date incomeDate = dateFormat.parse(dateStr);

                // Mettre à jour le solde
                userRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double solde = snapshot.getValue(Double.class);
                        if (solde == null) solde = 0.0;

                        userRef.child("solde").setValue(solde + montant);

                        // Enregistrer le revenu
                        DatabaseReference incomesRef = userRef.child("incomes").push();
                        Income income = new Income(montant, incomeDate, descStr);
                        incomesRef.setValue(income)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this, "Revenu enregistré", Toast.LENGTH_SHORT).show();
                                    bottomSheetDialog.dismiss();
                                    calculateFinancialHealthScore();
                                    loadRecentTransactions(); // Rafraîchir les transactions
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MainActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Erreur de lecture du solde", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (NumberFormatException e) {
                amount.setError("Montant invalide");
            } catch (ParseException e) {
                date.setError("Date invalide");
            }
        });
    }

    private void showBudgetDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_budget, null);
        dialog.setContentView(sheetView);
        dialog.show();

        EditText salaireInput = sheetView.findViewById(R.id.salaireInput);
        Button saveBudget = sheetView.findViewById(R.id.saveBudget);

        saveBudget.setOnClickListener(v -> {
            String salaireStr = salaireInput.getText().toString();
            if (salaireStr.isEmpty()) {
                salaireInput.setError("Montant requis");
                return;
            }

            try {
                double salaire = Double.parseDouble(salaireStr);
                userRef.child("soldeInitial").setValue(salaire);
                userRef.child("solde").setValue(salaire)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Solde initial défini", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            calculateFinancialHealthScore();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } catch (NumberFormatException e) {
                salaireInput.setError("Montant invalide");
            }
        });
    }

    private void loadCategoriesIntoSpinner(Spinner spinner) {
        userRef.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> categories = new ArrayList<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String category = categorySnapshot.getValue(String.class);
                    if (category != null) {
                        categories.add(category);
                    }
                }

                if (categories.isEmpty()) {
                    categories.add("Nourriture");
                    categories.add("Transport");
                    categories.add("Autres");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MainActivity.this,
                        android.R.layout.simple_spinner_item,
                        categories
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur de chargement des catégories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDefaultCategories() {
        userRef.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    String[] defaultCategories = {
                            "Alimentation", "Transport", "Logement",
                            "Loisirs", "Santé", "Éducation", "Autres"
                    };

                    for (String category : defaultCategories) {
                        userRef.child("categories").push().setValue(category);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur d'initialisation des catégories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHistoryButton() {
        LinearLayout historyAction = findViewById(R.id.historyAction);
        if (historyAction != null) {
            historyAction.setOnClickListener(v -> {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            });
        }
    }

    private void setupExpensesChart() {
        PieChart pieChart = findViewById(R.id.expenses_chart);
        TextView totalExpensesText = findViewById(R.id.totalExpensesText);

        userRef.child("expenses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Float> categorySums = new HashMap<>();
                float totalExpenses = 0;

                // Parcourir toutes les dépenses
                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    Expenses expense = expenseSnapshot.getValue(Expenses.class);
                    if (expense != null) {
                        String category = expense.getCategorie();
                        float amount = (float) expense.getMontant();

                        categorySums.put(category, categorySums.containsKey(category) ?
                                categorySums.get(category) + amount : amount);
                        totalExpenses += amount;
                    }
                }

                // Mettre à jour le total
                totalExpensesText.setText(String.format(Locale.getDefault(), "Total: %.2fdh", totalExpenses));

                // Créer les entrées pour le graphique
                List<PieEntry> entries = new ArrayList<>();
                for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                    entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                }

                if (entries.isEmpty()) {
                    pieChart.setNoDataText("Aucune donnée de dépenses disponible");
                    pieChart.invalidate();
                    return;
                }

                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.white));

                // Formatter les valeurs (pourcentages sans décimales)
                PieData pieData = new PieData(dataSet);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.getDefault(), "%.0f%%", value);
                    }
                });

                // Configuration du graphique
                pieChart.setData(pieData);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.setCenterText("Dépenses");
                pieChart.setCenterTextSize(14f);
                pieChart.setHoleRadius(40f);
                pieChart.setTransparentCircleRadius(45f);
                pieChart.setDrawEntryLabels(false); // Désactiver les labels dans le cercle

                // Afficher le nom de la catégorie au clic
                pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        if (e instanceof PieEntry) {
                            PieEntry pe = (PieEntry) e;

                        }
                    }

                    @Override
                    public void onNothingSelected() {}
                });

                // Configuration de la légende
                Legend legend = pieChart.getLegend();
                legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
                legend.setDrawInside(false);

                pieChart.animateY(1000);
                pieChart.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur de chargement des dépenses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier à nouveau si l'utilisateur est connecté
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Connexion.class));
            finish();
        }
    }

    // Classe interne pour formatter les pourcentages
    private static class PercentFormatter extends com.github.mikephil.charting.formatter.ValueFormatter {
        private final PieChart pieChart;

        public PercentFormatter(PieChart pieChart) {
            this.pieChart = pieChart;
        }

        @Override
        public String getFormattedValue(float value) {
            return String.format(Locale.getDefault(), "%.1f%%", value);
        }
    }
    // Déclarer en variable de classe
    private ValueEventListener budgetProgressListener;

    private void setupBudgetProgress() {
        TextView budgetRatioText = findViewById(R.id.textView);
        ProgressBar budgetProgressBar = findViewById(R.id.budgetProgressBar);
        TextView budgetUsedText = findViewById(R.id.budgetUsedText);
        TextView budgetLeftText = findViewById(R.id.budgetLeftText);

        // Supprimer l'ancien écouteur s'il existe
        if (budgetProgressListener != null) {
            userRef.removeEventListener(budgetProgressListener);
        }

        budgetProgressListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Récupérer le solde initial
                Double soldeInitial = snapshot.child("soldeInitial").getValue(Double.class);
                if (soldeInitial == null) soldeInitial = 0.0;

                // 2. Calculer le total des revenus
                double totalRevenus = 0;
                for (DataSnapshot incomeSnapshot : snapshot.child("incomes").getChildren()) {
                    Income income = incomeSnapshot.getValue(Income.class);
                    if (income != null) {
                        totalRevenus += income.getMontant();
                    }
                }

                // 3. Calculer le total des dépenses
                double totalDepenses = 0;
                for (DataSnapshot expenseSnapshot : snapshot.child("expenses").getChildren()) {
                    Expenses expense = expenseSnapshot.getValue(Expenses.class);
                    if (expense != null) {
                        totalDepenses += expense.getMontant();
                    }
                }

                // 4. Calcul des indicateurs
                double budgetTotal = soldeInitial + totalRevenus;
                double reste = budgetTotal - totalDepenses;
                int percentageUsed = budgetTotal > 0 ?
                        Math.min((int)((totalDepenses / budgetTotal) * 100), 100) : 0;

                // Création de copies finales pour le lambda
                final double fTotalDepenses = totalDepenses;
                final double fBudgetTotal = budgetTotal;
                final double fSoldeInitial = soldeInitial;
                final double fTotalRevenus = totalRevenus;
                final int fPercentageUsed = percentageUsed;
                final double fReste = reste;

                // 5. Mise à jour UI
                runOnUiThread(() -> {
                    budgetRatioText.setText(String.format(Locale.FRANCE,
                            "%.2f DH / %.2f DH",
                            fTotalDepenses, fBudgetTotal, fSoldeInitial, fTotalRevenus));

                    budgetProgressBar.setProgress(fPercentageUsed);
                    budgetUsedText.setText(String.format("%d%% utilisé", fPercentageUsed));

                    if (fReste < 0) {
                        budgetLeftText.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                        budgetLeftText.setText(String.format(Locale.FRANCE, "Dépassement: %.2f DH", -fReste));
                    } else {
                        budgetLeftText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.gray_616161));
                        budgetLeftText.setText(String.format(Locale.FRANCE, "Reste: %.2f DH", fReste));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Ajout de l'écouteur
        userRef.addValueEventListener(budgetProgressListener);
    }

    // Nettoyage dans onDestroy()
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (budgetProgressListener != null) {
            userRef.removeEventListener(budgetProgressListener);
        }
    }
    private void calculateFinancialHealthScore() {
        FinancialHealthCalculator.calculateFinancialHealth(userRef, new FinancialHealthCalculator.ScoreCalculationCallback() {

            @Override
            public void onScoreCalculated(int score, String level, int colorResId) {
                runOnUiThread(() -> {
                    TextView scoreValue = findViewById(R.id.score_value);
                    TextView scoreLevel = findViewById(R.id.score_level);
                    CircularProgressBar progressBar = findViewById(R.id.financial_score_progress);

                    if (scoreValue != null) scoreValue.setText(String.valueOf(score));
                    if (scoreLevel != null) {
                        scoreLevel.setText(level);
                        scoreLevel.setTextColor(ContextCompat.getColor(MainActivity.this, colorResId));
                    }
                    if (progressBar != null) {
                        progressBar.setProgress(score);
                        progressBar.setProgressBarColor(ContextCompat.getColor(MainActivity.this, colorResId));
                    }
                });
            }

            @Override
            public void onCalculationFailed(DatabaseError error) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Erreur de calcul du score", Toast.LENGTH_SHORT).show());
            }
        });


    }
    private void updateScoreDate() {
        TextView scoreDate = findViewById(R.id.score_date);
        if (scoreDate != null) {
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.FRANCE);
            String currentMonthYear = monthYearFormat.format(new Date()).toLowerCase();
            scoreDate.setText(currentMonthYear);
        }
    }
    private void loadRecentTransactions() {
        TextView clique = findViewById(R.id.clique);
        clique.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, TransactionHistoryActivity.class)));

        // Écouteur combiné pour dépenses et revenus
        ValueEventListener transactionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Transaction> allTransactions = new ArrayList<>();

                // Récupérer les 4 dernières dépenses
                List<DataSnapshot> expenseSnapshots = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.child("expenses").getChildren()) {
                    expenseSnapshots.add(snapshot);
                }
                Collections.reverse(expenseSnapshots); // Pour avoir les plus récentes en premier

                for (int i = 0; i < Math.min(4, expenseSnapshots.size()); i++) {
                    Expenses expense = expenseSnapshots.get(i).getValue(Expenses.class);
                    if (expense != null) {
                        allTransactions.add(new Transaction(
                                expense.getDate(),
                                expense.getDescription(),
                                expense.getCategorie(),
                                -expense.getMontant()
                        ));
                    }
                }

                // Récupérer les 4 derniers revenus
                List<DataSnapshot> incomeSnapshots = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.child("incomes").getChildren()) {
                    incomeSnapshots.add(snapshot);
                }
                Collections.reverse(incomeSnapshots);

                for (int i = 0; i < Math.min(4, incomeSnapshots.size()); i++) {
                    Income income = incomeSnapshots.get(i).getValue(Income.class);
                    if (income != null) {
                        allTransactions.add(new Transaction(
                                income.getDate(),
                                income.getDescription(),
                                "Revenu",
                                income.getMontant()
                        ));
                    }
                }

                // Trier toutes les transactions par date (récent -> ancien)
                Collections.sort(allTransactions, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                // Garder seulement les 4 plus récentes
                List<Transaction> recentTransactions = allTransactions.subList(0, Math.min(allTransactions.size(), 4));

                // Afficher
                displayRecentTransactions(recentTransactions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Erreur de chargement des transactions", Toast.LENGTH_SHORT).show();
            }
        };

        // Ajouter l'écouteur
        userRef.addValueEventListener(transactionsListener);
    }

    private void displayRecentTransactions(List<Transaction> transactions) {
        LinearLayout transactionsContainer = findViewById(R.id.transactions_list);
        if (transactionsContainer == null) return;

        // Supprimer toutes les vues existantes
        transactionsContainer.removeAllViews();

        if (transactions.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Aucune transaction récente");
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.gray));
            emptyView.setTextSize(14);
            emptyView.setPadding(16, 16, 16, 16);
            transactionsContainer.addView(emptyView);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.FRANCE);

        for (Transaction transaction : transactions) {
            // Créer la vue de transaction
            View transactionView = LayoutInflater.from(this).inflate(R.layout.item_transaction_card, transactionsContainer, false);

            // Configurer les éléments UI
            ImageView icon = transactionView.findViewById(R.id.transaction_icon);
            TextView description = transactionView.findViewById(R.id.transaction_description);
            TextView date = transactionView.findViewById(R.id.transaction_date);
            TextView amount = transactionView.findViewById(R.id.transaction_amount);
            CardView iconBackground = transactionView.findViewById(R.id.icon_background);

            // Définir l'icône et la couleur
            setTransactionIconAndColor(icon, iconBackground, transaction.getCategorie());

            description.setText(transaction.getDescription());
            date.setText(dateFormat.format(transaction.getDate()));

            if (transaction.getMontant() < 0) {
                amount.setText(String.format(Locale.FRANCE, "-%.2f DH", Math.abs(transaction.getMontant())));
                amount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                amount.setText(String.format(Locale.FRANCE, "+%.2f DH", transaction.getMontant()));
                amount.setTextColor(ContextCompat.getColor(this, R.color.green));
            }

            // Ajouter la vue au conteneur
            transactionsContainer.addView(transactionView);

            // Ajouter un séparateur si nécessaire
            if (transactions.indexOf(transaction) < transactions.size() - 1) {
                View separator = new View(this);
                separator.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                ));
                separator.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light));
                transactionsContainer.addView(separator);
            }
        }
    }

    private void setTransactionIconAndColor(ImageView icon, CardView background, String category) {
        int iconRes = R.drawable.ic_others;
        int bgColor = R.color.icon_gray_bg;
        int iconColor = R.color.gray;

        switch (category.toLowerCase()) {
            case "logement":
            case "loyer":
                iconRes = R.drawable.ic_home;
                bgColor = R.color.icon_purple_bg;
                iconColor = R.color.purple;
                break;
            case "alimentation":
            case "nourriture":
            case "restaurant":
                iconRes = R.drawable.ic_restaurant;
                bgColor = R.color.icon_red_bg;
                iconColor = R.color.red;
                break;
            case "transport":
            case "voiture":
            case "essence":
                iconRes = R.drawable.ic_transport;
                bgColor = R.color.icon_blue_bg;
                iconColor = R.color.blue;
                break;
            case "shopping":
            case "achats":
                iconRes = R.drawable.ic_shopping;
                bgColor = R.color.icon_pink_bg; // Rose clair
                iconColor = R.color.pink;
                break;
            case "revenu":
            case "salaire":
                iconRes = R.drawable.ic_bank_card;
                bgColor = R.color.icon_green_bg;
                iconColor = R.color.green;
                break;
            case "santé":
            case "médecin":
                iconRes = R.drawable.ic_sante;
                bgColor = R.color.icon_red_bg;
                iconColor = R.color.red;
                break;
            case "éducation":
            case "école":
                iconRes = R.drawable.ic_education;
                bgColor = R.color.icon_blue_bg;
                iconColor = R.color.blue;
                break;
            case "loisirs":
            case "hobby":
                iconRes = R.drawable.ic_hobbies;
                bgColor = R.color.icon_yellow_bg;
                iconColor = R.color.yellow_dark;
                break;
            case "assurances":
                iconRes = R.drawable.ic_assurances;
                bgColor = R.color.icon_purple_bg;
                iconColor = R.color.colorPrimaryDark;
                break;
            case "épargne":
            case "investissement":
                iconRes = R.drawable.ic_exchange_funds;
                bgColor = R.color.icon_teal_bg;
                iconColor = R.color.teal_dark;
                break;
            default:
                iconRes = R.drawable.ic_others;
                bgColor = R.color.icon_gray_bg;
                iconColor = R.color.gray;
        }

        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, iconColor));
        background.setCardBackgroundColor(ContextCompat.getColor(this, bgColor));
    }

}