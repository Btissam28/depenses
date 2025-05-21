package com.example.budget;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {
    private static final int EDIT_TRANSACTION_REQUEST = 1;

    private TableLayout tableHistorique;
    private SearchView searchView;
    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpenses;
    private List<Transaction> transactionList = new ArrayList<>();
    private SimpleDateFormat dateFormat;
    private double totalIncome = 0.0;
    private double totalExpenses = 0.0;
    private double totalBalance = 0.0;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        initializeViews();
        setupSearchView();
        loadTransactions();
    }

    private void initializeViews() {
        tableHistorique = findViewById(R.id.table_historique);
        searchView = findViewById(R.id.search_view);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpenses = findViewById(R.id.tv_total_expenses);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTransactions(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTransactions(newText);
                return true;
            }
        });
    }

    private void filterTransactions(String query) {
        if (query.isEmpty()) {
            displayTransactions(transactionList);
        } else {
            List<Transaction> filteredList = new ArrayList<>();
            for (Transaction transaction : transactionList) {
                if (transaction.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                        (transaction.getCategorie() != null && transaction.getCategorie().toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(transaction);
                }
            }
            displayTransactions(filteredList);
        }
    }

    private void loadTransactions() {
        totalIncome = 0.0;
        totalExpenses = 0.0;
        totalBalance = 0.0;
        transactionList.clear();

        Toast.makeText(this, "Chargement des transactions...", Toast.LENGTH_SHORT).show();

        DatabaseReference expensesRef = userRef.child("expenses");
        DatabaseReference incomesRef = userRef.child("incomes");
        DatabaseReference balanceRef = userRef.child("solde");

        expensesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    try {
                        Expenses expense = expenseSnapshot.getValue(Expenses.class);
                        if (expense != null) {
                            totalExpenses += expense.getMontant();

                            Transaction transaction = new Transaction(
                                    expense.getDate(),
                                    expense.getDescription(),
                                    expense.getCategorie(),
                                    -expense.getMontant()
                            );
                            transactionList.add(transaction);
                        }
                    } catch (Exception e) {
                        Toast.makeText(TransactionHistoryActivity.this,
                                "Erreur de lecture d'une dépense: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                updateExpensesDisplay();
                loadIncomes();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransactionHistoryActivity.this,
                        "Erreur de chargement des dépenses: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                loadIncomes();
            }
        });
    }

    private void loadIncomes() {
        userRef.child("incomes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot incomeSnapshot : snapshot.getChildren()) {
                    try {
                        Income income = incomeSnapshot.getValue(Income.class);
                        if (income != null) {
                            totalIncome += income.getMontant();

                            Transaction transaction = new Transaction(
                                    income.getDate(),
                                    income.getDescription(),
                                    "Revenu",
                                    income.getMontant()
                            );
                            transactionList.add(transaction);
                        }
                    } catch (Exception e) {
                        Toast.makeText(TransactionHistoryActivity.this,
                                "Erreur de lecture d'un revenu: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                updateIncomeDisplay();
                loadBalance();
                sortTransactionsByDate();
                displayTransactions(transactionList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransactionHistoryActivity.this,
                        "Erreur de chargement des revenus: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                sortTransactionsByDate();
                displayTransactions(transactionList);
            }
        });
    }

    private void loadBalance() {
        userRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        totalBalance = snapshot.getValue(Double.class);
                        updateBalanceDisplay();
                    } catch (Exception e) {
                        Toast.makeText(TransactionHistoryActivity.this,
                                "Erreur de lecture du solde: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TransactionHistoryActivity.this,
                            "Aucun solde trouvé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TransactionHistoryActivity.this,
                        "Erreur de chargement du solde: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBalanceDisplay() {
        String formattedBalance = String.format(Locale.getDefault(), "%.2f DH", totalBalance);
        tvTotalBalance.setText(formattedBalance);
    }

    private void updateIncomeDisplay() {
        String formattedIncome = String.format(Locale.getDefault(), "%.2f DH", totalIncome);
        tvTotalIncome.setText(formattedIncome);
    }

    private void updateExpensesDisplay() {
        String formattedExpenses = String.format(Locale.getDefault(), "%.2f DH", totalExpenses);
        tvTotalExpenses.setText(formattedExpenses);
    }

    private void sortTransactionsByDate() {
        Collections.sort(transactionList, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));
    }

    private void displayTransactions(List<Transaction> transactions) {
        tableHistorique.removeViews(1, Math.max(0, tableHistorique.getChildCount() - 1));

        if (transactions.isEmpty()) {
            Toast.makeText(this, "Aucune transaction trouvée", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            TableRow row = createTableRow(transaction, i);
            tableHistorique.addView(row);
        }
    }

    private TableRow createTableRow(Transaction transaction, int position) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
        row.setBackgroundResource(R.drawable.table_row_background);
        row.setPadding(6, 6, 6, 6);

        // Date Cell
        TextView dateCell = createTextView(dateFormat.format(transaction.getDate()),
                0.8f, R.color.black, 14);
        row.addView(dateCell);

        // Description Cell
        TextView descCell = createTextView(transaction.getDescription(),
                1.5f, R.color.black, 14);
        row.addView(descCell);

        // Category Cell
        TextView catCell = createTextView(transaction.getCategorie(),
                1.0f, R.color.gray, 14);
        row.addView(catCell);

        // Amount Cell
        TextView amountCell = createTextView(
                String.format(Locale.getDefault(), "%.2f DH", transaction.getMontant()),
                1.0f,
                transaction.getMontant() < 0 ? R.color.red : R.color.green,
                14);
        amountCell.setGravity(View.TEXT_ALIGNMENT_TEXT_END);
        row.addView(amountCell);

        // Edit Button
        ImageButton editButton = new ImageButton(this);
        editButton.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        editButton.setContentDescription("Modifier cette transaction");
        editButton.setOnClickListener(v -> openEditTransactionActivity(transaction, position));
        row.addView(editButton);

        return row;
    }

    private TextView createTextView(String text, float weight, int colorRes, float textSize) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight));
        textView.setPadding(4, 4, 4, 4);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
        textView.setTextSize(textSize);
        return textView;
    }

    private void openEditTransactionActivity(Transaction transaction, int position) {
        Intent editIntent = new Intent(this, EditTransactionActivity.class);
        editIntent.putExtra("transaction_index", position);
        editIntent.putExtra("transaction_date", transaction.getDate().getTime());
        editIntent.putExtra("transaction_description", transaction.getDescription());
        editIntent.putExtra("transaction_category", transaction.getCategorie());
        editIntent.putExtra("transaction_amount", transaction.getMontant());
        startActivityForResult(editIntent, EDIT_TRANSACTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_TRANSACTION_REQUEST && resultCode == RESULT_OK && data != null) {
            int position = data.getIntExtra("transaction_index", -1);

            if (data.getBooleanExtra("deleted", false)) {
                // Handle deletion
                handleTransactionDeletion(position, data.getDoubleExtra("transaction_amount", 0));
            } else {
                // Handle modification by reloading all transactions
                loadTransactions();
                Toast.makeText(this, "Transaction mise à jour avec succès", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleTransactionDeletion(int position, double amount) {
        if (position >= 0 && position < transactionList.size()) {
            // Update totals
            if (amount > 0) {
                totalIncome -= amount;
                updateIncomeDisplay();
            } else {
                totalExpenses += amount; // amount is negative for expenses
                updateExpensesDisplay();
            }

            totalBalance -= amount;
            updateBalanceDisplay();

            // Remove from list
            transactionList.remove(position);

            // Refresh display
            displayTransactions(transactionList);
            Toast.makeText(this, "Transaction supprimée avec succès", Toast.LENGTH_SHORT).show();
        }
    }
}