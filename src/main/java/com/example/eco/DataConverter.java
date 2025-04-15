package com.example.eco;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataConverter {
    // Chuyển đổi dữ liệu nhị phân sang chuỗi hexa
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString();
    }

    // Chuyển đổi bytes sang dữ liệu cảm biến cụ thể
    public static float bytesToTemperature(byte[] bytes) {
        // ESP32 thường sử dụng định dạng little-endian
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        short rawValue = buffer.getShort();

        // Chuyển đổi từ định dạng số cố định (hệ số chia 100)
        return rawValue / 100.0f;
    }
}
