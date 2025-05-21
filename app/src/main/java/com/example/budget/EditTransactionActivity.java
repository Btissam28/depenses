package com.example.budget;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditTransactionActivity extends AppCompatActivity {
    private EditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnDate, btnSave, btnDelete;
    private Calendar selectedDate = Calendar.getInstance();
    private boolean isIncome;
    private int transactionIndex;
    private double originalAmount;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialisation des vues
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnDate = findViewById(R.id.btn_date);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);

        // Récupération des données de la transaction
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            transactionIndex = extras.getInt("transaction_index");
            long dateMillis = extras.getLong("transaction_date");
            String description = extras.getString("transaction_description");
            String category = extras.getString("transaction_category");
            double amount = extras.getDouble("transaction_amount");

            originalAmount = amount;
            isIncome = amount > 0;

            // Configuration des vues avec les données existantes
            selectedDate.setTimeInMillis(dateMillis);
            updateDateButton();
            etDescription.setText(description);
            etAmount.setText(String.format(Locale.getDefault(), "%.2f", Math.abs(amount)));

            // Configuration du spinner de catégories
            setupCategorySpinner(isIncome, category);
        }

        // Gestion du sélecteur de date
        btnDate.setOnClickListener(v -> showDatePicker());

        // Gestion du bouton Enregistrer
        btnSave.setOnClickListener(v -> saveTransaction());

        // Gestion du bouton Supprimer
        btnDelete.setOnClickListener(v -> deleteTransaction());
    }

    private void setupCategorySpinner(boolean isIncome, String selectedCategory) {
        List<String> categories = new ArrayList<>();

        if (isIncome) {
            categories.add("Revenu");
        } else {
            categories.add("Alimentation");
            categories.add("Transport");
            categories.add("Logement");
            categories.add("Loisirs");
            categories.add("Santé");
            categories.add("Éducation");
            categories.add("Autre");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (selectedCategory != null && categories.contains(selectedCategory)) {
            spinnerCategory.setSelection(categories.indexOf(selectedCategory));
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateButton();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        btnDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void saveTransaction() {
        try {
            String description = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            if (description.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            if (!isIncome) {
                amount = -amount;
            }

            // Mettre à jour dans Firebase
            DatabaseReference ref = isIncome ? userRef.child("incomes") : userRef.child("expenses");

            // Pour simplifier, nous allons supprimer et recréer la transaction
            // Dans une vraie application, vous devriez avoir un ID unique pour chaque transaction
            double finalAmount = amount;
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int count = 0;
                    for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                        if (count == transactionIndex) {
                            // Supprimer l'ancienne transaction
                            transactionSnapshot.getRef().removeValue();

                            // Ajouter la nouvelle transaction
                            if (isIncome) {
                                Income income = new Income(Math.abs(finalAmount),
                                        selectedDate.getTime(),
                                        description);
                                ref.push().setValue(income);
                            } else {
                                Expenses expense = new Expenses(Math.abs(finalAmount),
                                        category,
                                        selectedDate.getTime(),
                                        description);
                                ref.push().setValue(expense);
                            }

                            // Mettre à jour le solde
                            updateBalance(originalAmount, finalAmount);
                            break;
                        }
                        count++;
                    }

                    // Retourner à l'activité précédente avec un résultat positif
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("transaction_index", transactionIndex);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EditTransactionActivity.this,
                            "Erreur lors de la mise à jour: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBalance(double oldAmount, double newAmount) {
        userRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double currentBalance = snapshot.getValue(Double.class);
                    double updatedBalance = currentBalance - oldAmount + newAmount;
                    userRef.child("solde").setValue(updatedBalance);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditTransactionActivity.this,
                        "Erreur de mise à jour du solde",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTransaction() {
        DatabaseReference ref = isIncome ? userRef.child("incomes") : userRef.child("expenses");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                    if (count == transactionIndex) {
                        // Supprimer la transaction
                        transactionSnapshot.getRef().removeValue();

                        // Mettre à jour le solde
                        updateBalance(originalAmount, 0);

                        // Retourner avec plus d'informations
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("transaction_index", transactionIndex);
                        resultIntent.putExtra("deleted", true);
                        resultIntent.putExtra("transaction_amount", originalAmount);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                        break;
                    }
                    count++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditTransactionActivity.this,
                        "Erreur lors de la suppression: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}