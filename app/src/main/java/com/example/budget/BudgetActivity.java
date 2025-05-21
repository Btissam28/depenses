package com.example.budget;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    // Constantes
    private static final String[] DEFAULT_CATEGORIES = {
            "Alimentation", "Transport", "Logement",
            "Loisirs", "Santé", "Éducation", "Autres"
    };
    private static final int[] QUICK_AMOUNTS = {50, 100, 200, 500};

    // Composants UI
    private EditText amountInput;
    private TextView budgetAvailableText;
    private Button createBudgetButton;
    private GridLayout categoryGrid;

    // Données
    private String selectedCategory = "";
    private final Map<String, Double> allocatedBudgets = new HashMap<>();
    private double monthlyBudget = 0.0;
    private double remainingBudget = 0.0;
    private boolean budgetLoaded = false;

    // Firebase
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        initializeFirebase();
        initializeViews();
        setupUI();
        loadUserData();
    }

    private void initializeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
    }

    private void initializeViews() {
        amountInput = findViewById(R.id.amount_input);
        budgetAvailableText = findViewById(R.id.budget_disponible);
        createBudgetButton = findViewById(R.id.create_budget_button);
        categoryGrid = findViewById(R.id.category_grid);
        createBudgetButton.setEnabled(false);
    }

    private void setupUI() {
        setupNavigation();
        setupQuickAmountButtons();
        setupNumpad();
        setupCategories();

        // Ajout du listener pour le bouton de création de budget
        createBudgetButton.setOnClickListener(v -> saveBudget(v));
    }

    private void setupNavigation() {
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.validate_button).setOnClickListener(v ->
                startActivity(new Intent(this, BudgetListActivity.class)));
        findViewById(R.id.cancel_button).setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        userRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double balance = snapshot.getValue(Double.class);
                if (balance != null) {
                    monthlyBudget = balance;
                    loadAllocatedBudgets();
                } else {
                    showBudgetNotSet();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Erreur de lecture du solde");
            }
        });
    }

    private void loadAllocatedBudgets() {
        userRef.child("budgets").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allocatedBudgets.clear();
                double totalAllocated = 0.0;

                for (DataSnapshot budgetSnapshot : snapshot.getChildren()) {
                    String category = budgetSnapshot.getKey();
                    Double amount = budgetSnapshot.getValue(Double.class);
                    if (category != null && amount != null) {
                        allocatedBudgets.put(category, amount);
                        totalAllocated += amount;
                    }
                }

                updateBudgetCalculations(totalAllocated);
                budgetLoaded = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Erreur de lecture des budgets");
            }
        });
    }

    private void updateBudgetCalculations(double totalAllocated) {
        remainingBudget = Math.max(0, monthlyBudget - totalAllocated);
        createBudgetButton.setEnabled(remainingBudget > 0);
        updateBudgetDisplay();
    }

    private void updateBudgetDisplay() {
        String text = String.format("Budget disponible à répartir: %.2f Dh sur %.2f Dh", remainingBudget, monthlyBudget);
        budgetAvailableText.setText(text);
    }

    private void showBudgetNotSet() {
        budgetAvailableText.setText("Aucun budget mensuel défini");
        Toast.makeText(this,
                "Définissez d'abord le solde dans la page principale",
                Toast.LENGTH_LONG).show();
        createBudgetButton.setEnabled(false);
    }

    private void setupQuickAmountButtons() {
        for (int i = 0; i < QUICK_AMOUNTS.length; i++) {
            int resId = getResources().getIdentifier(
                    "quick_amount_" + QUICK_AMOUNTS[i], "id", getPackageName());
            Button button = findViewById(resId);
            if (button != null) {
                int amount = QUICK_AMOUNTS[i];
                button.setOnClickListener(v -> addAmount(amount));
            }
        }
    }

    private void addAmount(int amount) {
        String current = amountInput.getText().toString();
        double newAmount = current.isEmpty() ? amount : Double.parseDouble(current) + amount;
        amountInput.setText(String.valueOf(newAmount));
    }

    private void setupNumpad() {
        for (int i = 0; i <= 9; i++) {
            int resId = getResources().getIdentifier("numpad_" + i, "id", getPackageName());
            Button button = findViewById(resId);
            if (button != null) {
                int digit = i;
                button.setOnClickListener(v -> appendDigit(digit));
            }
        }

        findViewById(R.id.numpad_dot).setOnClickListener(v -> appendDecimalPoint());
        findViewById(R.id.numpad_backspace).setOnClickListener(v -> deleteLastCharacter());
    }

    private void appendDigit(int digit) {
        String current = amountInput.getText().toString();
        amountInput.setText(current.equals("0") ? String.valueOf(digit) : current + digit);
    }

    private void appendDecimalPoint() {
        String current = amountInput.getText().toString();
        if (!current.contains(".")) {
            amountInput.setText(current.isEmpty() ? "0." : current + ".");
        }
    }

    private void deleteLastCharacter() {
        String current = amountInput.getText().toString();
        if (current.length() > 1) {
            amountInput.setText(current.substring(0, current.length() - 1));
        } else {
            amountInput.setText("0");
        }
    }

    private void setupCategories() {
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            View child = categoryGrid.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout categoryLayout = (LinearLayout) child;
                categoryLayout.setOnClickListener(v -> selectCategory(categoryLayout));
            }
        }
    }

    private void selectCategory(LinearLayout categoryLayout) {
        // Désélectionner toutes les catégories
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            View child = categoryGrid.getChildAt(i);
            if (child instanceof LinearLayout) {
                child.setSelected(false);
                child.setBackgroundResource(android.R.color.transparent);
            }
        }

        // Sélectionner la nouvelle catégorie
        categoryLayout.setSelected(true);
        categoryLayout.setBackgroundResource(R.drawable.category_selected_background);

        // Mettre à jour la catégorie sélectionnée
        TextView categoryText = (TextView) categoryLayout.getChildAt(1);
        selectedCategory = categoryText.getText().toString();

        // Afficher le budget existant s'il y en a un
        Double categoryBudget = allocatedBudgets.get(selectedCategory);
        amountInput.setText(String.valueOf(categoryBudget != null ? categoryBudget : "0"));
    }

    public void saveBudget(View view) {
        if (!budgetLoaded) {
            showError("Chargement du budget en cours...");
            return;
        }

        if (!validateBudgetInput()) return;

        double amount = Double.parseDouble(amountInput.getText().toString());
        double difference = calculateBudgetDifference(amount);

        if (difference > remainingBudget) {
            showBudgetExceededError();
            return;
        }

        saveBudgetToFirebase(amount);
    }

    private boolean validateBudgetInput() {
        if (selectedCategory.isEmpty()) {
            showError("Veuillez sélectionner une catégorie");
            return false;
        }

        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            showError("Montant requis");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < 0) {
                showError("Montant négatif non autorisé");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Montant invalide");
            return false;
        }

        return true;
    }

    private double calculateBudgetDifference(double newAmount) {
        Double currentAmount = allocatedBudgets.get(selectedCategory);
        return currentAmount != null ? newAmount - currentAmount : newAmount;
    }

    private void showBudgetExceededError() {
        String message = String.format(
                "Ce montant dépasse le budget restant à répartir (%.2f Dh)",
                remainingBudget);
        showError(message);
    }

    private void saveBudgetToFirebase(double amount) {
        userRef.child("budgets").child(selectedCategory).setValue(amount)
                .addOnSuccessListener(unused -> {
                    showSuccess("Budget de " + amount + " Dh alloué à " + selectedCategory);
                    resetForm();
                })
                .addOnFailureListener(e -> showError("Erreur lors de l'enregistrement"));
    }

    private void resetForm() {
        amountInput.setText("0");
        selectedCategory = "";
        deselectAllCategories();
    }

    private void deselectAllCategories() {
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            View child = categoryGrid.getChildAt(i);
            if (child instanceof LinearLayout) {
                child.setSelected(false);
                child.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}