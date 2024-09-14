package com.nlscan.scanhub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbReceiver extends BroadcastReceiver {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // Thiết bị USB đã được kết nối
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d("USB", "Thiết bị USB đã kết nối: " + device.getDeviceName());
                // Xử lý kết nối thiết bị ở đây
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // Thiết bị USB đã bị ngắt kết nối
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d("USB", "Thiết bị USB đã ngắt kết nối: " + device.getDeviceName());
                // Xử lý ngắt kết nối thiết bị ở đây
            }
        } else if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        // Quyền truy cập thiết bị đã được cấp
                        Log.d("USB", "Quyền truy cập USB đã được cấp cho thiết bị: " + device.getDeviceName());
                    }
                } else {
                    Log.d("USB", "Quyền truy cập USB bị từ chối cho thiết bị: " + device.getDeviceName());
                }
            }
        }
    }
}
