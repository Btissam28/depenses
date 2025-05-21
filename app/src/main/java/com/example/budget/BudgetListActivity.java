package com.example.budget;

import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BudgetListActivity extends AppCompatActivity {
    private TableLayout budgetTableLayout;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_list);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        budgetTableLayout = findViewById(R.id.budget_table_layout);
        loadBudgets();
    }

    private void loadBudgets() {
        userRef.child("budgets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String category = categorySnapshot.getKey();
                    Double amount = categorySnapshot.getValue(Double.class);

                    TableRow row = new TableRow(BudgetListActivity.this);
                    row.setBackgroundResource(R.drawable.table_row_border);

                    TextView categoryView = new TextView(BudgetListActivity.this);
                    categoryView.setText(category);
                    categoryView.setPadding(8, 8, 8, 8);
                    categoryView.setTextSize(16);
                    categoryView.setBackgroundResource(R.drawable.cell_border_right);

                    TextView amountView = new TextView(BudgetListActivity.this);
                    amountView.setText(String.format("%.2f Dh", amount));
                    amountView.setPadding(8, 8, 8, 8);
                    amountView.setTextSize(16);

                    row.addView(categoryView);
                    row.addView(amountView);
                    budgetTableLayout.addView(row);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                TableRow errorRow = new TableRow(BudgetListActivity.this);
                TextView errorText = new TextView(BudgetListActivity.this);
                errorText.setText("Erreur de lecture des budgets");
                errorText.setTextSize(16);
                errorRow.addView(errorText);
                budgetTableLayout.addView(errorRow);
            }
        });
    }
}