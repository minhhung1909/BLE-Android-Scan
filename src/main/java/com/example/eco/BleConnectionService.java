package com.example.eco;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

public class BleConnectionService {
    private static final String TAG = "BleConnectionService";

    private Context context;
    private BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;
    private ConnectionCallback connectionCallback;
    private boolean connected = false;

    // Interface để thông báo trạng thái kết nối
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onServicesDiscovered();
        void onDataReceived(byte[] data);
    }

    public boolean writeStringCharacteristic(String serviceUuid, String characteristicUuid, String data) {
        if (data == null) return false;
        return writeCharacteristic(serviceUuid, characteristicUuid, data.getBytes());
    }

    public BleConnectionService(Context context) {
        this.context = context;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    public void connect(BluetoothDevice device) {
        this.device = device;
        disconnect(); // Ngắt kết nối cũ nếu có

        if (!BlePermissionHelper.checkBlePermissions(context)) {
            Log.e(TAG, "Cannot connect: Missing Bluetooth permissions");
            return;
        }

        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
            Log.d(TAG, "Connecting to device: " + device.getAddress());
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during connect: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            if (!BlePermissionHelper.checkBlePermissions(context)) {
                Log.e(TAG, "Cannot disconnect: Missing Bluetooth permissions");
                return;
            }

            try {
                bluetoothGatt.disconnect();
                Log.d(TAG, "Disconnecting from GATT server");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception during disconnect: " + e.getMessage());
            }
        }
    }

    public void close() {
        if (bluetoothGatt != null) {
            if (!BlePermissionHelper.checkBlePermissions(context)) {
                Log.e(TAG, "Cannot close: Missing Bluetooth permissions");
                return;
            }

            try {
                bluetoothGatt.close();
                bluetoothGatt = null;
                Log.d(TAG, "Closed GATT connection");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception during close: " + e.getMessage());
            }
        }
    }

    // Updated write characteristic method using fixed UUIDs
    public boolean writeCharacteristic(String serviceUuid, String characteristicUuid, byte[] data) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "GATT not initialized");
            return false;
        }

        if (!BlePermissionHelper.checkBlePermissions(context)) {
            Log.e(TAG, "Missing Bluetooth permissions");
            return false;
        }

        try {
            // Use hardcoded UUIDs from BleConstants
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstants.SERVICE_ESP32));
            if (service == null) {
                Log.e(TAG, "Service not found: " + BleConstants.SERVICE_ESP32);
                return false;
            }

            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(BleConstants.CHARACTERISTIC_ESP32_RX));
            if (characteristic == null) {
                Log.e(TAG, "Characteristic not found: " + BleConstants.CHARACTERISTIC_ESP32_RX);
                return false;
            }

            // Log data for debugging
            Log.d(TAG, "Writing data to characteristic: " + BleConstants.CHARACTERISTIC_ESP32_RX);
            Log.d(TAG, "Data (String): " + new String(data));
            Log.d(TAG, "Data (Hex): " + bytesToHex(data));

            characteristic.setValue(data);
            return bluetoothGatt.writeCharacteristic(characteristic);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during write: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error writing characteristic: " + e.getMessage());
            return false;
        }
    }

    // Helper method to convert bytes to hex for logging
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    // Enable notifications with permission checks
    public boolean enableNotifications(String serviceUuid, String characteristicUuid) {
        if (bluetoothGatt == null) return false;

        if (!BlePermissionHelper.checkBlePermissions(context)) {
            Log.e(TAG, "Cannot enable notifications: Missing Bluetooth permissions");
            return false;
        }

        try {
            // Use hardcoded UUIDs
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstants.SERVICE_ESP32));
            if (service == null) return false;

            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(BleConstants.CHARACTERISTIC_ESP32_TX));
            if (characteristic == null) return false;

            // Enable local notifications
            bluetoothGatt.setCharacteristicNotification(characteristic, true);

            // Write to descriptor to enable remote notifications
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(BleConstants.DESCRIPTOR_CONFIG));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return bluetoothGatt.writeDescriptor(descriptor);
            }
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception enabling notifications: " + e.getMessage());
            return false;
        }
    }

    // Modified GATT callback with improved logging
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                Log.d(TAG, "Connected to GATT server");
                if (!BlePermissionHelper.checkBlePermissions(context)) {
                    Log.e(TAG, "Cannot discover services: Missing Bluetooth permissions");
                    return;
                }

                try {
                    Log.d(TAG, "Attempting to start service discovery");
                    bluetoothGatt.discoverServices();
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception during service discovery: " + e.getMessage());
                }

                if (connectionCallback != null) {
                    connectionCallback.onConnected();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                Log.d(TAG, "Disconnected from GATT server");
                if (connectionCallback != null) {
                    connectionCallback.onDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully");

                // Log all discovered services for debugging
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "Service found: " + service.getUuid());
                    for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                        Log.d(TAG, "  Characteristic: " + ch.getUuid());
                    }
                }

                if (connectionCallback != null) {
                    connectionCallback.onServicesDiscovered();
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                Log.d(TAG, "Read data from: " + characteristic.getUuid());
                if (connectionCallback != null) {
                    connectionCallback.onDataReceived(data);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            Log.d(TAG, "Received notification from: " + characteristic.getUuid());
            Log.d(TAG, "Data (String): " + new String(data));

            if (connectionCallback != null) {
                connectionCallback.onDataReceived(data);
            }
        }
        public boolean isConnected() {
            return bluetoothGatt != null && connected;
        }



    };



}