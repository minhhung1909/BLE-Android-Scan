package com.example.eco;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco.R;

import java.util.List;

public class DeviceControlActivity extends AppCompatActivity implements BleConnectionService.ConnectionCallback {
    private BleConnectionService bleService;
    private TextView dataTextView;
    private EditText inputEditText;
    private Button sendButton;
    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        // Kiểm tra quyền truy cập
        if (!BlePermissionHelper.checkBlePermissions(this)) {
            BlePermissionHelper.requestBlePermissions(this);
        }
        verifyDatabaseStructure();

        // Khởi tạo views
        dataTextView = findViewById(R.id.dataTextView);
        inputEditText = findViewById(R.id.inputEditText);
        sendButton = findViewById(R.id.sendButton);
        Button homeButton = findViewById(R.id.homeButton);

        // Khởi tạo BLE service
        bleService = new BleConnectionService(this);
        bleService.setConnectionCallback(this);

        // Lấy thiết bị từ intent hoặc từ bộ nhớ
        device = getIntent().getParcelableExtra("device");

        // Nếu có lệnh gửi nhưng không có thiết bị, chuyển về WelcomeActivity
        String command = getIntent().getStringExtra("sendCommand");
        if (device == null && command != null) {
            Toast.makeText(this, "Vui lòng kết nối thiết bị trước", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Nếu có thiết bị, kết nối
        if (device != null) {
            bleService.connect(device);

            // Nếu có lệnh để gửi
            if (command != null) {
                byte[] data = command.getBytes();
                // Chờ một chút để kết nối thành công trước khi gửi lệnh
                new Handler().postDelayed(() -> {
                    if (bleService != null) {
                        bleService.writeCharacteristic(
                                BleConstants.SERVICE_ESP32,
                                BleConstants.CHARACTERISTIC_ESP32_RX,
                                data
                        );
                        dataTextView.append("Gửi lệnh: " + command + "\n");
                    }
                }, 1000);
            }
        }

        // Thiết lập nút gửi
        sendButton.setOnClickListener(v -> {
            String text = inputEditText.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                byte[] data = text.getBytes();
                boolean success = bleService.writeCharacteristic(
                        BleConstants.SERVICE_ESP32,
                        BleConstants.CHARACTERISTIC_ESP32_RX,
                        data
                );

                if (success) {
                    Toast.makeText(this, "Đã gửi: " + text, Toast.LENGTH_SHORT).show();
                    inputEditText.setText("");  // Xóa nội dung đã nhập
                } else {
                    Toast.makeText(this, "Lỗi gửi dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceControlActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleService.close();
    }

    // Các phương thức callback
    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Đã kết nối", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(DeviceControlActivity.this, ControlActivity.class);
//            startActivity(intent);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Đã ngắt kết nối", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onServicesDiscovered() {
        bleService.enableNotifications(
                BleConstants.SERVICE_ESP32,
                BleConstants.CHARACTERISTIC_ESP32_TX
        );
    }

    @Override
    public void onDataReceived(byte[] data) {
        final String text = new String(data);
        Log.e("DeviceControlActivity", "Raw data received: " + text);

        try {
            // Parse data - assuming format "V:12.5,I:0.5"
            float voltage = 0;
            float current = 0;

            String[] parts = text.split(",");
            Log.e("DeviceControlActivity", "Split parts: " + parts.length);
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("V:")) {
                    voltage = Float.parseFloat(part.substring(2));
                } else if (part.startsWith("I:")) {
                    current = Math.abs(Float.parseFloat(part.substring(2))) ;
                }
            }

            // Save data to the database
            BleDataDbHelper dbHelper = new BleDataDbHelper(this);
            dbHelper.insertData(voltage, current); // Ensure `insertData` is implemented in `BleDataDbHelper`
            dbHelper.close();

            // Broadcast the new data
            Intent powerUpdateIntent = new Intent("com.example.simpleble.POWER_UPDATE");
            powerUpdateIntent.putExtra("VOLTAGE_VALUE", voltage);
            powerUpdateIntent.putExtra("CURRENT_VALUE", current);
            Log.e("DeviceControlActivity", "Broadcasting power update: V=" + voltage + ", I=" + current);
            sendBroadcast(powerUpdateIntent);

        } catch (Exception e) {
            Log.e("DeviceControlActivity", "Error parsing data: " + e.getMessage());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BlePermissionHelper.REQUEST_BLE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                // Reconnect with the device since we now have permissions
                if (device != null && bleService != null) {
                    bleService.connect(device);
                }
            } else {
                Toast.makeText(this, "BLE permissions are required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void verifyDatabaseStructure() {
        BleDataDbHelper dbHelper = new BleDataDbHelper(this);
        // Verify database exists
        String dbPath = getDatabasePath(dbHelper.getDatabaseName()).getAbsolutePath();
        Log.e("DATABASE_CHECK", "Database path: " + dbPath);

        // Verify reading works
        List<BleDataDbHelper.EnergyDataRecord> records = dbHelper.getAllData();
        Log.e("DATABASE_CHECK", "Total records: " + records.size());
        dbHelper.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to WelcomeActivity
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}