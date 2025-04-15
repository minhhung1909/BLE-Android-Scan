package com.example.eco;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BleCommandService extends Service implements BleConnectionService.ConnectionCallback {
    private static final String TAG = "BleCommandService";

    public static final String ACTION_SEND_COMMAND = "com.example.simpleble.ACTION_SEND_COMMAND";
    private BleConnectionService bleService;
    private Handler handler = new Handler();
    private String pendingCommand;
    private String deviceAddress;
    private int retryCount = 0;
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        bleService = new BleConnectionService(this);
        bleService.setConnectionCallback(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SEND_COMMAND.equals(intent.getAction())) {
            String command = intent.getStringExtra("command");
            deviceAddress = intent.getStringExtra("deviceAddress");

            if (command != null) {
                Log.d(TAG, "Received command: " + command);
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bleService != null) {
            bleService.close();
        }
    }

    private void writeCommandToDevice(String command) {
        if (command == null) return;

        byte[] data = command.getBytes();
        boolean success = bleService.writeCharacteristic(
                BleConstants.SERVICE_ESP32,
                BleConstants.CHARACTERISTIC_ESP32_RX,
                data
        );

        if (success) {
            Log.d(TAG, "Command sent successfully: " + command);
            pendingCommand = null;

            // Allow time for command processing before disconnecting
            handler.postDelayed(() -> {
                bleService.disconnect();
                stopSelf();
            }, 1000);
        } else {
            handleWriteFailure();
        }
    }

    private void handleWriteFailure() {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            Log.d(TAG, "Write failed, retrying in 2 seconds... (Attempt " + retryCount + ")");

            handler.postDelayed(() -> {
                if (pendingCommand != null) {
                    writeCommandToDevice(pendingCommand);
                }
            }, RETRY_DELAY);
        } else {
            Log.e(TAG, "Failed to send command after " + MAX_RETRY + " attempts");
            bleService.disconnect();
            stopSelf();
        }
    }

    // BleConnectionService.ConnectionCallback methods
    @Override
    public void onConnected() {
        Log.d(TAG, "Connected to device");
        retryCount = 0;
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Disconnected from device");
    }

    @Override
    public void onServicesDiscovered() {
        Log.d(TAG, "Services discovered");
        if (pendingCommand != null) {
            writeCommandToDevice(pendingCommand);
        }
    }

    @Override
    public void onDataReceived(byte[] data) {
        String response = new String(data);
        Log.d(TAG, "Data received: " + response);
        // Broadcast the response if needed
    }
}