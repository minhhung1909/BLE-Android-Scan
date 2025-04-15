package com.example.eco;

import android.os.Build;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ControlActivity extends AppCompatActivity {
    private boolean buttonState = false;
    private BleConnectionService bleConnection;
    private TextView voltageValueText;
    private TextView currentValueText;
    private TextView powerValueText;
    private LineChart powerChart;
    private String currentTimeRange = "hour";
    private BroadcastReceiver powerUpdateReceiver;

    // Add a handler and runnable for periodic updates
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;
    private final int UPDATE_INTERVAL = 1000; // Update every 1 second
    private static final String PREFS_NAME = "BlePrefs";
    private static final String KEY_CHARGING_STATE = "isCharging";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity);

        voltageValueText = findViewById(R.id.voltageValueText);
        currentValueText = findViewById(R.id.currentValueText);
        powerValueText = findViewById(R.id.powerValueText);
        powerChart = findViewById(R.id.powerChart);
        bleConnection = new BleConnectionService(this);

        // Initialize chart
        setupChart();

        // Load initial data
        loadChartData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize the receiver
        powerUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract values directly from intent
                float voltage = intent.getFloatExtra("VOLTAGE_VALUE", 0);
                float current = intent.getFloatExtra("CURRENT_VALUE", 0);

                Log.d("ControlActivity", "Broadcast received: V=" + voltage + ", I=" + current);

                // Update UI directly with fresh values
                updatePowerReadings(voltage, current);

                // Then update chart
                loadChartData();
            }
        };


        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter("com.example.simpleble.POWER_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerUpdateReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(powerUpdateReceiver, filter);
        }

        // Start periodic updates
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(powerUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("ControlActivity", "Receiver not registered", e);
        }
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(powerUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("ControlActivity", "Receiver not registered", e);
        }
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to WelcomeActivity
        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadChartData() {
        BleDataDbHelper dbHelper = new BleDataDbHelper(this);
        List<BleDataDbHelper.EnergyDataRecord> allRecords = dbHelper.getAllData();
        List<BleDataDbHelper.EnergyDataRecord> records;

        // Get the last 20 records for display (newest records)
        if (allRecords.size() <= 20) {
            records = allRecords;
        } else {
            // Since getAllData returns records in descending order (newest first),
            // we take the first 20 records
            records = allRecords.subList(0, 20);
        }

        Log.d("ControlActivity", "Retrieved " + records.size() + " records from database");

        // Create chart data points
        ArrayList<Entry> powerEntries = new ArrayList<>();

        // Plot the data points with newer records later in the chart
        // (reverse the order to show chronological progression)
        for (int i = records.size() - 1; i >= 0; i--) {
            BleDataDbHelper.EnergyDataRecord record = records.get(i);
            float power = record.voltage * record.current;
            powerEntries.add(new Entry(records.size() - 1 - i, power));
            Log.d("ControlActivity", "Record: " + record.voltage + ", " + record.current + ", Power: " + power);
        }

        if (powerEntries.isEmpty()) {
            Log.e("ControlActivity", "No power entries to display");
            powerChart.setData(null);
            powerChart.invalidate();
            return;
        }

        LineDataSet powerDataSet = new LineDataSet(powerEntries, "Công suất (W)");
        styleDataSet(powerDataSet, Color.BLUE);

        LineData lineData = new LineData(powerDataSet);
        powerChart.setData(lineData);
        powerChart.invalidate();
    }

    // Helper method to update the UI with power readings
    private void updatePowerReadings(float voltage, float current) {
        if (voltageValueText != null && currentValueText != null && powerValueText != null) {
            voltageValueText.setText(String.format(Locale.getDefault(), "%.2f V", voltage));
            currentValueText.setText(String.format(Locale.getDefault(), "%.2f A", current));
            float power = voltage * current;
            powerValueText.setText(String.format(Locale.getDefault(), "%.2f W", power));
            Log.d("ControlActivity", "UI updated: V=" + voltage + ", I=" + current + ", P=" + power);
        } else {
            Log.e("ControlActivity", "TextViews not initialized");
        }
    }

    private void setupChart() {
        // Basic chart setup
        powerChart.getDescription().setEnabled(false);
        powerChart.setDrawGridBackground(false);
        powerChart.setTouchEnabled(true);
        powerChart.setDragEnabled(true);
        powerChart.setScaleEnabled(true);
        powerChart.setPinchZoom(true);

        // X-axis setup
        XAxis xAxis = powerChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Format X-axis to show position numbers (1, 2, 3...)
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value + 1); // Start from 1
            }
        });

        // Other chart styling
        powerChart.getLegend().setEnabled(true);
        powerChart.animateX(1000);
    }

    // Add this method to style the dataset for the chart
    private void styleDataSet(LineDataSet dataSet, int color) {
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(color);
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(Color.rgb(255, 165, 0));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    }

}