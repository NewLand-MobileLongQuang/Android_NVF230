package com.nlscan.scanhub;

import static com.nlscan.scanhub.MainActivity.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nlscan.nlsdk.NLDevice;
import com.nlscan.nlsdk.NLDeviceStream;
import com.nlscan.nlsdk.NLUartStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LQscanActivity extends AppCompatActivity {


    private byte[] barcodeBuff = new byte[2 * 1024];
    private int barcodeLen = 0;
    private int checkedItem = 0;
    private long lastNGTime = 0;
    private long lastNonNGTime = 0;
    private  boolean CheckOK = false;
    private int sequenceNumber1 = 1;
    private int sequenceNumber2 = 1;
    private int delta = 3600;


    private NLDeviceStream ds = new NLDevice(NLDeviceStream.DevClass.DEV_COMPOSITE);

    private EditText lqetResult1;
    private EditText lqetResult2;

    private TextView tvLogTitle;
    private Button btnToggleLog;
    private boolean isLogVisible = true;

    private TextView tvBaoOK, tvBaoNG, tvTotal;
    private Button btnStart, btnClear, btnExit;
    MediaPlayer mediaPlayerERR;
    MediaPlayer mediaPlayerSUC;
    private boolean permissionsAreOk = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lqscan);

        lqetResult1 = findViewById(R.id.lqetResult1);
        lqetResult2 = findViewById(R.id.lqetResult2);
        tvLogTitle = findViewById(R.id.tvLogTitle);
        btnToggleLog = findViewById(R.id.btnToggleLog);

        tvBaoOK = findViewById(R.id.tvBaoOK);
        tvBaoNG = findViewById(R.id.tvBaoNG);
        tvTotal = findViewById(R.id.tvTotal);

        btnStart = findViewById(R.id.btnStart);
        btnClear = findViewById(R.id.btnClear);
        btnExit = findViewById(R.id.btnExit);
        mediaPlayerERR = MediaPlayer.create(this, R.raw.error1);
        mediaPlayerSUC = MediaPlayer.create(this, R.raw.beep);
        permissionsAreOk = checkPermissions();
        setTitle("LQ Scan");

        btnStart.setOnClickListener(view -> {
            OnOpenCloseDevice();
        });

        // Ẩn hiện cột Log
        btnToggleLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLogVisible) {
                    lqetResult2.setVisibility(View.GONE);
                    tvLogTitle.setVisibility(View.GONE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lqetResult1.getLayoutParams();
                    params.weight = 2;
                    btnToggleLog.setText("HIỆN LOG");
                    lqetResult1.setLayoutParams(params);
                } else {
                    lqetResult2.setVisibility(View.VISIBLE);
                    tvLogTitle.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lqetResult1.getLayoutParams();
                    params.weight = 1;
                    btnToggleLog.setText("ẨN LOG");
                    lqetResult1.setLayoutParams(params);
                }
                isLogVisible = !isLogVisible;
            }
        });

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

            btnCancel.setOnClickListener(view -> dialog.dismiss());
            btnOk.setOnClickListener(view -> {
                String deltaInput = etDelta.getText().toString();
                if (!deltaInput.isEmpty()) {
                    delta = Integer.parseInt(deltaInput);
                }
                dialog.dismiss();

            });

            dialog.show();
        });



        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btnStart.setText("Bắt đầu");
                lqetResult1.setText("");
                lqetResult2.setText("");
                tvBaoOK.setText("Số lượng bao OK: 0");
                tvBaoNG.setText("Số lượng bao NG: 0");
                tvTotal.setText("Tổng số lượng bao: 0");
                sequenceNumber1 = 1;
                sequenceNumber2 =1;
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
    void showText(String showtime, String text, int color) {

        String sequence;
        if(sequenceNumber1<=9)
            sequence = String.format("%3d.   ", sequenceNumber1++);
        else
        {
            if(sequenceNumber1<=99) {
                sequence = String.format("%3d.  ", sequenceNumber1++);
            }
            else
                sequence = String.format("%3d. ", sequenceNumber1++);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(sequence);

        int tabStopPosition = 500;
        builder.setSpan(new TabStopSpan.Standard(tabStopPosition), 0, builder.length(), 0);


        builder.append(showtime + " - ");

        int colorStart = builder.length()-showtime.length()-3;

        builder.append(text);

        builder.setSpan(new android.text.style.ForegroundColorSpan(color), colorStart, builder.length(), 0);

        lqetResult1.append(builder);
        lqetResult1.append("\n");
    }

    /**
     * Table 2
     */
    void showText2(String showtime, String text, int color) {

        String sequence;
        if(sequenceNumber2<=9)
            sequence = String.format("%3d.   ", sequenceNumber2++);
        else
        {
            if(sequenceNumber2<=99) {
                sequence = String.format("%3d.  ", sequenceNumber2++);
            }
            else
                sequence = String.format("%3d. ", sequenceNumber2++);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(sequence);

        int tabStopPosition = 500;
        builder.setSpan(new TabStopSpan.Standard(tabStopPosition), 0, builder.length(), 0);

        builder.append(showtime + " - ");

        int colorStart = builder.length()-showtime.length()-3;
        builder.append(text);

        builder.setSpan(new android.text.style.ForegroundColorSpan(color), colorStart, builder.length(), 0);

        lqetResult2.append(builder);
        lqetResult2.append("\n");
    }

    void OnOpenCloseDevice() {
         {
            btnStart.setText("Bắt đầu");

            if (!ds.nl_OpenDevice(this, new NLDeviceStream.NLUsbListener() {

                @Override
                public void actionUsbPlug(int event) {
                    if (event == 1) {
                        ShowToast(getString(R.string.TextInfoPlugin));

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
                        //String prefix = String.format("scanBarCode len:%s data: ", barcodeLen);
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

                                int baoOK = Integer.parseInt(tvBaoOK.getText().toString().replaceAll("\\D+", ""));
                                int baoNG = Integer.parseInt(tvBaoNG.getText().toString().replaceAll("\\D+", ""));
                                int total;

                                    if (str.equals("NG")) {
                                        showText2(showtime, str ,Color.RED);
                                        if ((currentTime - lastNGTime > delta) && (currentTime - lastNonNGTime > delta)) {
                                            if (mediaPlayerERR != null) {
                                                if (!mediaPlayerERR.isPlaying()) {
                                                    mediaPlayerERR.start();
                                                }
                                            }
                                            baoNG++;
                                            showText(showtime, str,Color.RED);
                                            lastNGTime = currentTime;
                                        }
                                    } else {
                                        showText2(showtime,  str.substring(str.length() - 15),Color.BLACK);

                                        lastNonNGTime = currentTime;

                                        if (mediaPlayerERR != null && mediaPlayerERR.isPlaying()) {
                                            mediaPlayerERR.pause();
                                            mediaPlayerERR.seekTo(0);
                                        }

                                        if (mediaPlayerSUC != null) {
                                            if (!mediaPlayerSUC.isPlaying()) {
                                                mediaPlayerSUC.start();
                                            }
                                        }

                                        baoOK++;
                                        showText(showtime,  str.substring(str.length() - 15),Color.BLACK);
                                    }

                                    total= baoOK + baoNG;

                                    tvBaoOK.setText("Số lượng bao OK: " + baoOK);
                                    tvBaoNG.setText("Số lượng bao NG: " + baoNG);
                                    tvTotal.setText("Tổng số lượng bao: " + total);

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
