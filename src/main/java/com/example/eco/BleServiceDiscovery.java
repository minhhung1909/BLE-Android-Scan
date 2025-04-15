package com.example.eco;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.List;

public class BleServiceDiscovery {
    // Danh sách UUID được tìm thấy động
    private static String discoveredServiceUuid;
    private static String discoveredCharacteristicRxUuid;
    private static String discoveredCharacteristicTxUuid;

    public static void discoverServicesAndCharacteristics(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            String serviceUuid = service.getUuid().toString();
            Log.d("BleDiscovery", "Tìm thấy Service: " + serviceUuid);

            // Lưu service UUID đầu tiên phát hiện được (hoặc theo điều kiện khác)
            if (discoveredServiceUuid == null) {
                discoveredServiceUuid = serviceUuid;
            }

            // Tìm các characteristics
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                String charUuid = characteristic.getUuid().toString();
                Log.d("BleDiscovery", "Tìm thấy Characteristic: " + charUuid);

                // Lưu lại các characteristics có thuộc tính phù hợp
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    discoveredCharacteristicRxUuid = charUuid;
                }
                if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0 ||
                        (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    discoveredCharacteristicTxUuid = charUuid;
                }
            }
        }
    }

    // Getters
    public static String getServiceUuid() {
        return discoveredServiceUuid;
    }

    public static String getRxCharacteristicUuid() {
        return discoveredCharacteristicRxUuid;
    }

    public static String getTxCharacteristicUuid() {
        return discoveredCharacteristicTxUuid;
    }
}