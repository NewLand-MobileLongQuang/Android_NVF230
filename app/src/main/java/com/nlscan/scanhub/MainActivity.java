package com.nlscan.scanhub;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.nlscan.nlsdk.NLDevice;
import com.nlscan.nlsdk.NLDeviceStream;
import com.nlscan.nlsdk.NLUartStream;
import com.nlscan.scanhub.databinding.ActivityMainBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {
    private byte[] barcodeBuff = new byte[2 * 1024];
    private int barcodeLen = 0;

    private NLDeviceStream ds = new NLDevice(NLDeviceStream.DevClass.DEV_COMPOSITE);
    private String deviceInfo = null;
    static String TAG = "NLCOMM-DEMO";
    private int REQ_CODE_SELECT_FW = 0x1823;
    private boolean usbOpenChecked = false;
    private boolean permissionsAreOk = false;
    private int checkedItem = 0;
    String newFwPath;
    ActivityMainBinding binding;
    Button bnClearResult;
    Button bnScanBarcode;
    Button bnGetDeviceInfo;
    Button bnRestartDevice;
    EditText editConfig;
    Button bnQueCfg;
    Button bnOpenDevice;
    EditText etResult;
    Button bnUpdateFirmware;
    TextView txtFilePath;
    Button bnScanEnable;
    Button bnSenseMode;
    Button bnGetImg;
    Button bnEncode;
    Button bnLQScan;
    Button bnUpdateConfig;
    TextView editSerialname;
    TextView editBaud;
    ProgressBar pbUpdate;
    Spinner spCommtype;
    MediaPlayer mediaPlayerERR;
    MediaPlayer mediaPlayerSUC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        bnLQScan = binding.bnLQScan;
        bnClearResult = binding.bnClearResult;
        bnScanBarcode = binding.bnScanBarcode;
        bnGetDeviceInfo = binding.bnGetDeviceInfo;
        bnRestartDevice = binding.bnRestartDevice;
        editConfig = binding.editConfig;
        bnQueCfg = binding.bnQueCfg;
        bnOpenDevice = binding.bnOpenDevice;
        etResult = binding.etResult;
        bnUpdateFirmware = binding.bnUpdateFirmware;
        txtFilePath = binding.txtFilePath;
        bnScanEnable = binding.bnScanEnable;
        bnSenseMode = binding.bnSenseMode;
        bnGetImg = binding.bnGetImg;
        bnEncode = binding.bnEncode;
        bnUpdateConfig = binding.bnUpdateConfig;
        editSerialname = binding.editSerialname;
        editBaud = binding.editBaud;
        pbUpdate = binding.pbUpdate;
        spCommtype = binding.spCommtype;

        onClickEvent();
        permissionsAreOk = checkPermissions();
        setEnable(false);
        etResult.setFocusable(false);
        etResult.setFocusableInTouchMode(false);
        setTitle("N-ScanHub SDK " + ds.nl_GetSdkVersion());
        mediaPlayerERR = MediaPlayer.create(this, R.raw.error1);
        mediaPlayerSUC = MediaPlayer.create(this, R.raw.beep);
        bnLQScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to start LQscanActivity
                Intent intent = new Intent(MainActivity.this, LQscanActivity.class);
                startActivity(intent);
            }
        });

    }

    public void setting(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            }
        }
    }

    private void onClickEvent() {
        //R.id.bnOpenDevice
        bnOpenDevice.setOnClickListener(view -> {
            OnOpenCloseDevice();
        });
        //R.id.spCommtype
        spCommtype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                onCommselect(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //R.id.bnClearResult
        bnClearResult.setOnClickListener(view -> {
            OnClearResult();
        });
        //R.id.bnGetDeviceInfo
        bnGetDeviceInfo.setOnClickListener(view -> {
            OnGetDeviceInfo();
        });
        //R.id.bnRestartDevice
        bnRestartDevice.setOnClickListener(view -> {
            OnRestartDevice();
        });
        //R.id.bnScanBarcode
        bnScanBarcode.setOnClickListener(view -> {
            OnScanBarcode();
        });
        //R.id.bnScanEnable
        bnScanEnable.setOnClickListener(view -> {
            onScanEnable();
        });
        //R.id.bnSenseMode
        bnSenseMode.setOnClickListener(view -> {
            onSetSenseMode();
        });
        //R.id.bnEncode
        bnEncode.setOnClickListener(view -> {
            showSingleChoiceDialog();
        });

        // LQ scan
        bnLQScan.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, com.nlscan.scanhub.LQscanActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayerERR != null) {
            mediaPlayerERR.release();
            mediaPlayerERR = null;
        }

        if (mediaPlayerSUC != null) {
            mediaPlayerSUC.release();
            mediaPlayerSUC = null;
        }
        if (ds != null)
            ds.nl_CloseDevice();
        super.onDestroy();
    }

    private void setEnable(boolean enable) {
        bnScanBarcode.setEnabled(enable);
        bnGetDeviceInfo.setEnabled(enable);
        bnRestartDevice.setEnabled(enable);

        bnQueCfg.setEnabled(enable);
        editConfig.setEnabled(enable);
        bnUpdateFirmware.setEnabled(enable);

        bnScanEnable.setEnabled(enable);
        bnSenseMode.setEnabled(enable);
        bnGetImg.setEnabled(enable);
        bnUpdateConfig.setEnabled(enable);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setting(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
            case 2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {    // authorized
                    permissionsAreOk = checkPermissions();
                    if (permissionsAreOk) {
                        Log.i(TAG, "sdcard permission is ok.");
                    }
                } else {                                                        //permission denied
                    finish();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void ShowToast(CharSequence toastText) {
        @SuppressLint("InflateParams") View toastRoot = getLayoutInflater().inflate(R.layout.toast, null);
        TextView message = toastRoot.findViewById(R.id.toast_message);
        message.setText(toastText);

        Toast toastStart = new Toast(this);
        toastStart.setGravity(Gravity.CENTER, 0, 10);
        toastStart.setDuration(Toast.LENGTH_SHORT);
        toastStart.setView(toastRoot);
        toastStart.show();
    }

    private void showSingleChoiceDialog() {
        final String[] items = getResources().getStringArray(R.array.encode_item);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.output_barcode_encode))
                .setSingleChoiceItems(items, checkedItem, (dialogInterface, i) -> {
                    checkedItem = i;
                    dialogInterface.dismiss();
                });
        builder.create().show();
    }

    Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
        @Override
        public void subscribe(ObservableEmitter<Integer> emitter) {
            emitter.onNext(1);
        }
    });

    Consumer<Integer> usbRecvObserver = new Consumer<Integer>() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void accept(Integer integer) {
            if (checkedItem == 1) {
              showText(String.format("scanBarCode len:%s data: ", barcodeLen), new String(barcodeBuff, 0, barcodeLen, StandardCharsets.UTF_8));
            } else if (checkedItem == 2) {

                showText(String.format("scanBarCode len:%s data: ", barcodeLen), new String(barcodeBuff, 0, barcodeLen, Charset.forName("gb18030")));
            } else {

                showText(String.format("scanBarCode len:%s data: ", barcodeLen), new String(barcodeBuff, 0, barcodeLen));
            }
        }
    };

    Consumer<Integer> usbPlugObserver = new Consumer<Integer>() {
        @Override
        public void accept(Integer integer) {
            MainActivity.this.ShowToast(getString(R.string.TextInfoPlugout));
            //ds.close();
            usbOpenChecked = false;
            bnOpenDevice.setText(R.string.TextOpen);
            setEnable(false);
        }
    };

    //    @OnClick({R.id.bnOpenDevice})
    void OnOpenCloseDevice() {
        deviceInfo = null;
        Log.d(TAG, "OnOpenCloseDevice  " + usbOpenChecked);
        if (usbOpenChecked) {
            ds.nl_CloseDevice();
            setEnable(false);
            usbOpenChecked = false;
            bnOpenDevice.setText(R.string.TextOpen);
            return;
        }

        if (ds.nl_GetDevObj().getClass().equals(NLUartStream.class)) {
            String serialName = editSerialname.getText().toString();
            String baudString = editBaud.getText().toString();
            int baud;

            if (serialName.isEmpty() || baudString.isEmpty() || !serialName.startsWith("/dev/tty")) {
                Log.e(TAG, "Serial name or baud is error!");
                return;
            }


            try {
                baud = Integer.parseInt(baudString);
            } catch (NumberFormatException e) {
                return;
            }

            if (!ds.nl_OpenDevice(serialName, baud, new NLDeviceStream.NLUartListener() {
                @Override
                public void actionRecv(byte[] recvBuff, int len) {
                    barcodeLen = len;
                    if (usbOpenChecked) {
                        System.arraycopy(recvBuff, 0, barcodeBuff, 0, len);
                        observable.subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(usbRecvObserver);
                    }
                }

            })) {
                bnOpenDevice.setText(R.string.TextOpen);
                usbOpenChecked = false;
                return;
            }
        } else {
            // Listen to the USB unplugging event, and notify the main_activity thread when the USB is unplugged
            if (!ds.nl_OpenDevice(this, new NLDeviceStream.NLUsbListener() {
                @Override
                public void actionUsbPlug(int event) {
                    if (event == 1) {
                        MainActivity.this.ShowToast(getString(R.string.TextInfoPlugin));
                    } else {
                        ds.nl_CloseDevice();
                        MainActivity.this.ShowToast(getString(R.string.TextInfoPlugout));
                        observable.subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(usbPlugObserver);
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void actionUsbRecv(byte[] recvBuff, int len) {
                    barcodeLen = len;
                    if (usbOpenChecked) {
                        System.arraycopy(recvBuff, 0, barcodeBuff, 0, len);
                        String prefix = String.format("scanBarCode len:%s data: ", barcodeLen);
                        String str;
                        if (checkedItem == 1) {
                            str = new String(barcodeBuff, 0, barcodeLen, StandardCharsets.UTF_8);
                        } else if (checkedItem == 2) {
                            str = new String(barcodeBuff, 0, barcodeLen, Charset.forName("gb18030"));
                        } else {
                            str = new String(barcodeBuff, 0, barcodeLen);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (barcodeLen < 5) {
                                    if (mediaPlayerERR != null) {
                                        if (!mediaPlayerERR.isPlaying()) {
                                            mediaPlayerERR.start();
                                        }
                                    }
                                    showText("", "KHONG QUET DUOC");
                                } else {
                                    if (mediaPlayerERR != null && mediaPlayerERR.isPlaying()) {
                                        mediaPlayerERR.pause();
                                        mediaPlayerERR.seekTo(0);
                                    }

                                    if (mediaPlayerSUC != null) {
                                        if (!mediaPlayerSUC.isPlaying()) {
                                            mediaPlayerSUC.start();
                                        }
                                    }

                                    showText(prefix, str);
                                }
                            }
                        });
                    }
                }

            })) {
                bnOpenDevice.setText(R.string.TextOpen);
                usbOpenChecked = false;
                return;
            }
        }
        bnOpenDevice.setText(R.string.TextClose);
        usbOpenChecked = true;
        setEnable(true);
    }
    //    @OnItemSelected(R.id.spCommtype)
    void onCommselect(int position) {
        Log.d(TAG, "onCommselect: " + position);
        switch (position) {
            case 0:
                ds = new NLDevice(NLDeviceStream.DevClass.DEV_CDC);
                editSerialname.setEnabled(false);
                editBaud.setEnabled(false);
                break;
            case 1:
                ds = new NLDevice(NLDeviceStream.DevClass.DEV_POS);
                editSerialname.setEnabled(false);
                editBaud.setEnabled(false);
                break;
            case 2:
                ds = new NLDevice(NLDeviceStream.DevClass.DEV_COMPOSITE);
                editSerialname.setEnabled(false);
                editBaud.setEnabled(false);
                break;
            case 3:
                ds = new NLDevice(NLDeviceStream.DevClass.DEV_UART);
                editSerialname.setEnabled(true);
                editBaud.setEnabled(true);
                break;
        }
    }

    void OnClearResult() {
        etResult.setText("");
    }
//    @OnClick(R.id.bnGetDeviceInfo)
    void OnGetDeviceInfo() {
        deviceInfo = ds.nl_GetDeviceInfo();
        msgbox((deviceInfo != null ? deviceInfo : ""), "Device Information");
    }

//    @OnClick(R.id.bnRestartDevice)
    void OnRestartDevice() {
        ds.nl_RestartDevice();
        //ds.close();
        bnOpenDevice.setText(R.string.TextOpen);
    }

//    @OnClick(R.id.bnScanBarcode)
    void OnScanBarcode() {
        if (!scanBarCode()) {
            showText("OnScanBarcode", "failed");
        }
    }

//    @OnClick(R.id.bnScanEnable)
    void onScanEnable() {
        if (!ds.nl_SendCommand("SCNENA1")) {
            ShowToast(getString(R.string.TextConfigErr));
        }
    }

    void onSetSenseMode() {
        if (!ds.nl_SendCommand("SCNMOD2")) {
            ShowToast(getString(R.string.TextConfigErr));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SELECT_FW && resultCode == RESULT_OK) {
            List<String> list = data.getStringArrayListExtra("paths");
            if (list != null && list.size() > 0) {
                newFwPath = list.get(0);
                txtFilePath.setText(newFwPath);
            }
        }
    }

    boolean scanBarCode() {
        if (!ds.nl_DeviceIsOpen()) {
            return false;
        }
        return ds.nl_StartScan();
    }

    void showText(String prefix, String text) {
            etResult.append(prefix);
            etResult.append(text);
            etResult.append("\n");
    }

    void msgbox(String text, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(text);
        if (title != null) builder.setTitle(title);
        builder.setPositiveButton("OK", null);
        builder.setCancelable(true);
        builder.create().show();
    }
}
