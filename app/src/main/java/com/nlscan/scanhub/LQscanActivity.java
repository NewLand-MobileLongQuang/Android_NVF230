package com.nlscan.scanhub;

import static com.nlscan.scanhub.MainActivity.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nlscan.nlsdk.NLDevice;
import com.nlscan.nlsdk.NLDeviceStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class LQscanActivity extends AppCompatActivity {


    private byte[] barcodeBuff = new byte[2 * 1024];
    private int barcodeLen = 0;
    private int checkedItem = 0;
    private long lastNGTime = 0;
    private long lastGOODTime = 0;
    private long lastWAITTime = 0;

    //private LocalDateTime lastWAITTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
    private  boolean CheckOK = false;
    private int sequenceNumberMain =0;
    private int sequenceNumberLog =0;
    private int delta = 3500;
    private static final String PREFS_NAME = "ConfigPrefs";
    private static final String DELTA_KEY = "delta_key";
    private static final String PREFS_NAME_Main = "MyAppPreferences";
    private static final String DELTA_KEY_Main = "main_key";
    private static final String PREFS_NAME_Log = "logPreferences";
    private static final String DELTA_KEY_Log = "log_key";


    private NLDeviceStream ds = new NLDevice(NLDeviceStream.DevClass.DEV_COMPOSITE);

    private EditText lqetResultMain;
    private EditText lqetResultLog;

    private TextView tvLogTitle;
    private Button btnToggleLog;
    private boolean isLogVisible = true;

    private TextView tvBaoOK, tvBaoNG, tvTotal;
    private Button btnStart, btnClear, btnExit;
    MediaPlayer mediaPlayerERR;
    MediaPlayer mediaPlayerSUCC;
    private boolean permissionsAreOk = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lqscan);

        lqetResultMain = findViewById(R.id.lqetResultMain);
        lqetResultLog = findViewById(R.id.lqetResultLog);
        tvLogTitle = findViewById(R.id.tvLogTitle);
        btnToggleLog = findViewById(R.id.btnToggleLog);

        tvBaoOK = findViewById(R.id.tvBaoOK);
        tvBaoNG = findViewById(R.id.tvBaoNG);
        tvTotal = findViewById(R.id.tvTotal);

        btnStart = findViewById(R.id.btnStart);
        btnClear = findViewById(R.id.btnClear);
        btnExit = findViewById(R.id.btnExit);
        mediaPlayerERR = MediaPlayer.create(this, R.raw.error1);
        mediaPlayerSUCC = MediaPlayer.create(this, R.raw.beep);
        permissionsAreOk = checkPermissions();
        OnConnectToDevice();
        setTitle("LQ Scan");

        btnStart.setOnClickListener(view -> {
            OnConnectToDevice();
        });

        // Ẩn hiện cột Log
        btnToggleLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLogVisible) {
                    lqetResultLog.setVisibility(View.GONE);
                    tvLogTitle.setVisibility(View.GONE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lqetResultMain.getLayoutParams();
                    params.weight = 2;
                    btnToggleLog.setText("HIỆN LOG");
                    lqetResultMain.setLayoutParams(params);
                } else {
                    lqetResultLog.setVisibility(View.VISIBLE);
                    tvLogTitle.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lqetResultMain.getLayoutParams();
                    params.weight = 1;
                    btnToggleLog.setText("ẨN LOG");
                    lqetResultMain.setLayoutParams(params);
                }
                isLogVisible = !isLogVisible;
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        delta = sharedPreferences.getInt(DELTA_KEY, 3500);

        Button btnConfig = findViewById(R.id.btnconfig);

        btnConfig.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.popupconfig, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            EditText etDelta = dialogView.findViewById(R.id.etDelta);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnOk = dialogView.findViewById(R.id.btnOk);

            etDelta.setText(String.valueOf(delta));

            btnCancel.setOnClickListener(view -> dialog.dismiss());
            btnOk.setOnClickListener(okView -> {
                // Lưu giá trị vào SharedPreferences khi người dùng nhấn OK
                String deltaStr = etDelta.getText().toString();
                if (!deltaStr.isEmpty()) {
                    delta = Integer.parseInt(deltaStr);

                    // Lưu giá trị delta vào SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(DELTA_KEY, delta);
                    editor.apply();
                }
                dialog.dismiss();
            });

            dialog.show();
        });



        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckOK=false;
                ds.nl_SendCommand("SCNENA0");
                btnStart.setText("Bắt đầu");
                lqetResultMain.setText("");
                lqetResultLog.setText("");
                tvBaoOK.setText("Số lượng bao OK: 0");
                tvBaoNG.setText("Số lượng bao NG: 0");
                tvTotal.setText("Tổng số lượng bao: 0");
                sequenceNumberMain = 1;
                sequenceNumberLog = 1;
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void onClick(View view) {
        // Create an Intent to start LQscanActivity
        Intent intent = new Intent(LQscanActivity.this, MainActivity.class);
        startActivity(intent);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
            case 2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsAreOk = checkPermissions();
                    if (permissionsAreOk) {
                        Log.i(TAG, "sdcard permission is ok.");
                    }
                } else {
                    finish();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * TABLE 1
     * */
    void showTextMain(String showtime, String text, int color) {

        SharedPreferences sharedPreferencesdeltamain = getSharedPreferences(PREFS_NAME_Main, MODE_PRIVATE);
        sequenceNumberMain = sharedPreferencesdeltamain.getInt(DELTA_KEY_Main, 0);

        sequenceNumberMain++;
        SharedPreferences.Editor editor = sharedPreferencesdeltamain.edit();
        editor.putInt(DELTA_KEY_Main, sequenceNumberMain);
        editor.apply();

        String sequence;
        if(sequenceNumberMain<=9)
            sequence = String.format("%3d.   ", sequenceNumberMain);
        else
        {
            if(sequenceNumberMain<=99) {
                sequence = String.format("%3d.  ", sequenceNumberMain);
            }
            else
                sequence = String.format("%3d. ", sequenceNumberMain);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(sequence);

        builder.append(showtime + " - ");

        int colorStart = builder.length()-showtime.length()-3;

        builder.append(text);

        builder.setSpan(new android.text.style.ForegroundColorSpan(color), colorStart, builder.length(), 0);

        lqetResultMain.append(builder);
        lqetResultMain.append("\n");
    }

    /**
     * Table 2
     */
    void showTextLOG(String showtime, String text, int color) {

        SharedPreferences sharedPreferencesdeltaLog = getSharedPreferences(PREFS_NAME_Log, MODE_PRIVATE);
        sequenceNumberLog = sharedPreferencesdeltaLog.getInt(DELTA_KEY_Log, 0);

        sequenceNumberLog++;
        SharedPreferences.Editor editor = sharedPreferencesdeltaLog.edit();
        editor.putInt(DELTA_KEY_Main, sequenceNumberLog);
        editor.apply();

        String sequence;
        if(sequenceNumberLog <=9)
            sequence = String.format("%3d.   ", sequenceNumberLog);
        else
        {
            if(sequenceNumberLog <=99) {
                sequence = String.format("%3d.  ", sequenceNumberLog);
            }
            else
                sequence = String.format("%3d. ", sequenceNumberLog);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(sequence);

        builder.append(showtime + " - ");

        int colorStart = builder.length()-showtime.length()-3;
        builder.append(text);

        builder.setSpan(new android.text.style.ForegroundColorSpan(color), colorStart, builder.length(), 0);

        lqetResultLog.append(builder);
        lqetResultLog.append("\n");
    }

    void OnConnectToDevice() {
         {
             Log.d("LONGLOGCHECK", "OnConnectToDevice: ==========> ");
             if(!CheckOK){
                 btnStart.setText("Bắt đầu");
                 ds.nl_SendCommand("SCNENA0");
                 CheckOK=true;
             }
             else{
                 ds.nl_SendCommand("SCNENA1");
                 btnStart.setText("Đang quét");
             }


            if (!ds.nl_OpenDevice(this, new NLDeviceStream.NLUsbListener() {
                @Override
                public void actionUsbPlug(int event) {
                    if (event == 1) {
                        ShowToast("Device Plugged");
                    }
                    else {
                        ShowToast("Device UnPlugged");
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void actionUsbRecv(byte[] recvBuff, int len) {
                        barcodeLen = len;

                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                        String showtime = sdf.format(new Date());
                        long currentTime = System.currentTimeMillis();

                        System.arraycopy(recvBuff, 0, barcodeBuff, 0, len);

                        String strRecv;
                        if (checkedItem == 1) {
                            strRecv = new String(barcodeBuff, 0, barcodeLen, StandardCharsets.UTF_8);
                        } else if (checkedItem == 2) {
                            strRecv = new String(barcodeBuff, 0, barcodeLen, Charset.forName("gb18030"));
                        } else {
                            strRecv = new String(barcodeBuff, 0, barcodeLen);
                        }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            int baoOK = Integer.parseInt(tvBaoOK.getText().toString().replaceAll("\\D+", ""));
                            int baoNG = Integer.parseInt(tvBaoNG.getText().toString().replaceAll("\\D+", ""));
                            int total;

                            if(strRecv.contains("SCNENA0"))
                            {
                                showTextLOG(showtime, "Dừng quét" ,Color.BLACK);
                            }
                            else
                            {
                                if(strRecv.contains("SCNENA1"))
                                {
                                    showTextLOG(showtime, "Bắt đầu quét" ,Color.BLACK);
                                }
                                else
                                {
                                    if (strRecv.equals("NG")) {
                                        showTextLOG(showtime, strRecv ,Color.RED);
                                        if ((currentTime - lastNGTime > delta) && (currentTime - lastGOODTime > delta)&&(currentTime - lastWAITTime > delta)) {
                                            if (mediaPlayerERR != null) {
                                                if (!mediaPlayerERR.isPlaying()) {
                                                    mediaPlayerERR.start();
                                                }
                                            }
                                            baoNG++;
                                            showTextMain(showtime, strRecv,Color.RED);
                                            lastNGTime = currentTime;
                                        }
                                    }
                                    else
                                    {
                                        if (strRecv.contains("ETEM_WAIT"))
                                        {
                                            showTextLOG(showtime, strRecv.substring(strRecv.length() - 10) ,Color.BLACK);
                                            lastWAITTime = currentTime;
                                        }
                                        else
                                        {
                                            showTextLOG(showtime,  strRecv.substring(strRecv.length() - 15),Color.BLACK);

                                            lastGOODTime = currentTime;

                                            if (mediaPlayerERR != null && mediaPlayerERR.isPlaying())
                                            {
                                                mediaPlayerERR.pause();
                                                mediaPlayerERR.seekTo(0);
                                            }
                                            if (mediaPlayerSUCC != null) {
                                                if (!mediaPlayerSUCC.isPlaying()) {
                                                    mediaPlayerSUCC.start();
                                                }
                                            }

                                            baoOK++;
                                            showTextMain(showtime,  strRecv.substring(strRecv.length() - 15),Color.BLACK);
                                        }
                                    }
                                }

                                    total= baoOK + baoNG;

                                    tvBaoOK.setText("Số lượng bao OK: " + baoOK);
                                    tvBaoNG.setText("Số lượng bao NG: " + baoNG);
                                    tvTotal.setText("Tổng số lượng bao: " + total);
                                }
                        }
                    });
                }

            }))
            {
                return;
            }
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

}
