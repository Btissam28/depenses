package com.example.budget;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private DatabaseReference userRef;
    private TextView tvRevenuMois, tvDepenseMois;
    private LineChart lineChart;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        tvRevenuMois = findViewById(R.id.tv_revenu_mois);
        tvDepenseMois = findViewById(R.id.tv_depense_mois);
        lineChart = findViewById(R.id.line_chart);
        pieChart = findViewById(R.id.pie_chart);

        setupLineChart();
        setupPieChart();
        loadMonthlyData();
        loadExpensesByCategory();
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final String[] moisInitiales = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < moisInitiales.length) {
                    return moisInitiales[index];
                }
                return "";
            }
        });
    }

    private void setupPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDrawEntryLabels(false);
        pieChart.setUsePercentValues(true);
    }

    private void loadMonthlyData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                double revenuMois = 0;
                double depenseMois = 0;

                for (DataSnapshot incomeSnapshot : snapshot.child("incomes").getChildren()) {
                    Income income = incomeSnapshot.getValue(Income.class);
                    if (income != null && isCurrentMonth(income.getDate(), currentMonth, currentYear)) {
                        revenuMois += income.getMontant();
                    }
                }

                for (DataSnapshot expenseSnapshot : snapshot.child("expenses").getChildren()) {
                    Expenses expense = expenseSnapshot.getValue(Expenses.class);
                    if (expense != null && isCurrentMonth(expense.getDate(), currentMonth, currentYear)) {
                        depenseMois += expense.getMontant();
                    }
                }

                tvRevenuMois.setText(String.format(Locale.getDefault(), "%.2f DH", revenuMois));
                tvDepenseMois.setText(String.format(Locale.getDefault(), "%.2f DH", depenseMois));
                prepareLineChartData(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur
            }
        });
    }

    private boolean isCurrentMonth(Date date, int currentMonth, int currentYear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MONTH) == currentMonth &&
                cal.get(Calendar.YEAR) == currentYear;
    }

    private void prepareLineChartData(DataSnapshot snapshot) {
        Map<Integer, Float> monthlyExpenses = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            monthlyExpenses.put(i, 0f);
        }

        for (DataSnapshot expenseSnapshot : snapshot.child("expenses").getChildren()) {
            Expenses expense = expenseSnapshot.getValue(Expenses.class);
            if (expense != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(expense.getDate());
                int month = cal.get(Calendar.MONTH);
                monthlyExpenses.put(month, monthlyExpenses.get(month) + (float)expense.getMontant());
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            entries.add(new Entry(i, monthlyExpenses.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Dépenses mensuelles");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
        lineChart.animateY(1000);
    }

    private void loadExpensesByCategory() {
        userRef.child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Float> categorySums = new HashMap<>();

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    Expenses expense = expenseSnapshot.getValue(Expenses.class);
                    if (expense != null) {
                        String category = expense.getCategorie();
                        float amount = (float) expense.getMontant();
                        categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount);
                    }
                }

                List<PieEntry> entries = new ArrayList<>();
                for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                    if (entry.getValue() > 0) {
                        entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    }
                }

                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(12f);

                PieData pieData = new PieData(dataSet);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.getDefault(), "%.0f%%", value);
                    }
                });
                pieChart.setData(pieData);
                pieChart.invalidate();
                pieChart.animateY(1000);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur
            }
        });
    }
}