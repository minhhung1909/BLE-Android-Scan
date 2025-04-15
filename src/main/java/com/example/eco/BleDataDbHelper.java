package com.example.eco;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BleDataDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "BleDataDbHelper";
    private static final String DATABASE_NAME = "bleEnergyData";
    private static final int DATABASE_VERSION = 1;

    // Table and columns
    private static final String TABLE_BLE_DATA = "energy_data";
    private static final String KEY_ID = "id";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_VOLTAGE = "voltage";
    private static final String KEY_CURRENT = "current";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";

    public BleDataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_BLE_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_DEVICE_ADDRESS + " TEXT,"
                + KEY_VOLTAGE + " REAL,"
                + KEY_CURRENT + " REAL,"
                + KEY_DATE + " TEXT,"
                + KEY_TIME + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLE_DATA);
        onCreate(db);
    }

    public void insertData(float voltage, float current) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Get current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String currentTime = timeFormat.format(new Date());

        // Use the correct column names
        values.put(KEY_VOLTAGE, voltage);
        values.put(KEY_CURRENT, current);
        values.put(KEY_DATE, currentDate);
        values.put(KEY_TIME, currentTime);

        // Use the correct table name constant
        db.insert(TABLE_BLE_DATA, null, values);
        db.close();
    }

    public List<EnergyDataRecord> getAllData() {
        List<EnergyDataRecord> dataList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_BLE_DATA + " ORDER BY " + KEY_DATE + " DESC, " + KEY_TIME + " DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    EnergyDataRecord record = new EnergyDataRecord();
                    record.id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                    record.deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEVICE_ADDRESS));
                    record.voltage = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_VOLTAGE));
                    record.current = cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_CURRENT));
                    record.date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE));
                    record.time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME));

                    dataList.add(record);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return dataList;
    }

    // Data model class
    public static class EnergyDataRecord {
        public long id;
        public String deviceAddress;
        public float voltage;
        public float current;
        public String date;
        public String time;
    }
}