package com.example.eco;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.eco.R;

import java.util.List;


public class BleDeviceAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> deviceList;
    private int layout;

    public BleDeviceAdapter(Context context, int layout, List<BluetoothDevice> deviceList) {
        this.context = context;
        this.layout = layout;
        this.deviceList = deviceList;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, null);
        }

        TextView deviceName = convertView.findViewById(R.id.nameBLE);
        TextView statusdevice = convertView.findViewById(R.id.statusConnect);

        BluetoothDevice device = deviceList.get(position);

        // Set device name or address if name is null
        String name = device.getName();
        deviceName.setText(name != null ? name : device.getAddress());

        // For simplicity, showing all devices as not connected
        // You would need to implement actual connection state tracking
        statusdevice.setText("Not Connected");

        return convertView;
    }
}
