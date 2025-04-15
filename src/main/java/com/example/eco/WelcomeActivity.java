package com.example.eco;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eco.R;


public class WelcomeActivity extends AppCompatActivity {

    private Button settingsButton;
    private Button controlButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        settingsButton = findViewById(R.id.settingsButton);
        controlButton = findViewById(R.id.scanButton);

        // Mở cài đặt Bluetooth và sau đó mở màn hình scan
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(scanIntent);
            }
        });

        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if BLE is connected before opening ControlActivity
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                boolean isBluetoothEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();

                if (isBluetoothEnabled) {
                    Intent intent = new Intent(WelcomeActivity.this, ControlActivity.class);
                    startActivity(intent);
                } else {
                    // Show dialog to enable Bluetooth
                    Toast.makeText(WelcomeActivity.this,
                            "Hãy kết nối Bluetooth trước", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

}
