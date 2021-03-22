package com.databaker.bakervoiceconvertandroid;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.databaker.voiceconvert.VoiceConvertManager;
import com.databaker.voiceconvert.callback.AudioOutPutCallback;
import com.databaker.voiceconvert.callback.AuthCallback;
import com.databaker.voiceconvert.callback.ErrorCallback;
import com.databaker.voiceconvert.callback.SpeechCallback;
import com.databaker.voiceconvert.callback.WebSocketOpenCallback;
import com.databaker.voiceconvert.util.Utils;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 16000;

    private final String[] voiceDescArray = new String[]{"变声娇娇", "变声天天", "变声恐龙贝克", "变声乐迪", "变声未眠"};
    private final String[] voiceNameArray = new String[]{"Vc_jiaojiao", "Vc_tiantian", "Vc_baklong", "Vc_ledi", "Vc_weimian"};

    private String mVCFilePath = "";
    private String currentVoiceName = voiceNameArray[0];
    private boolean isRecording = false;
    private BufferedSink bufferedSink = null;
    private Button btnRecord = null;
    private Button btnFileRecord = null;
    private AudioTrack audioTrack = null;
    // 是否使用自己的音频传入，true代表自行录音进行录音转换，false代表使用SDK内部的录音机进行录音转换
    private boolean isUseCustomAudioData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String clientId = getSharedPreferences(AuthActivity.SP_NAME, MODE_PRIVATE).getString(AuthActivity.CLIENT_ID, "");
        String clientSecret = getSharedPreferences(AuthActivity.SP_NAME, MODE_PRIVATE).getString(AuthActivity.CLIENT_SECRET, "");

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        } else {
            ProgressDialog dialog = ProgressDialog.show(this, "正在授权", "授权中...");
            VoiceConvertManager.getInstance().auth(getApplicationContext(),
                    clientId,
                    clientSecret, new AuthCallback() {
                        @Override
                        public void onResult(boolean result) {
                            if (result) {
                                getSharedPreferences(AuthActivity.SP_NAME, MODE_PRIVATE).edit().putString(AuthActivity.CLIENT_ID, clientId).apply();
                                getSharedPreferences(AuthActivity.SP_NAME, MODE_PRIVATE).edit().putString(AuthActivity.CLIENT_SECRET, clientSecret).apply();
                                dialog.dismiss();
                                initView();
                            } else {
                                Toast.makeText(MainActivity.this, "鉴权失败", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                                finish();
                            }
                        }
                    });
        }
    }

    private void initView() {
        mVCFilePath = getExternalFilesDir("").getAbsolutePath() + File.separator + "vc.pcm";

        TextView tvVoiceName = findViewById(R.id.tvvcn);
        btnRecord = findViewById(R.id.btnRecord);
        btnFileRecord = findViewById(R.id.btnFileRecord);
        SwitchCompat switchCompat = findViewById(R.id.recordSwitch);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isUseCustomAudioData = isChecked;
            }
        });

        MaterialSpinner spinner = findViewById(R.id.spinner);
        spinner.setItems(voiceDescArray);
        spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                currentVoiceName = voiceNameArray[position];
                tvVoiceName.setText("当前音色：" + voiceDescArray[position]);
                VoiceConvertManager.getInstance().setVoiceName(currentVoiceName);
            }
        });


        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().equals("停止播放")) {
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                    btnRecord.setText("开始录音");
                    btnFileRecord.setEnabled(true);
                    return;
                }

                if (!isRecording) {


                    btnFileRecord.setEnabled(false);

                    if (!isUseCustomAudioData) {
                        VoiceConvertManager.getInstance().setSaveRecordFile();
                        btnRecord.setEnabled(false);
                        btnRecord.setText("正在连接服务器...");
                        VoiceConvertManager.getInstance().startRecord(new SpeechCallback() {
                            @Override
                            public void canSpeech() {
                                isRecording = true;
                                runOnUiThread(() -> {
                                            Toast.makeText(MainActivity.this, "请开始说话", Toast.LENGTH_SHORT).show();
                                            btnRecord.setText("停止录音");
                                            btnRecord.setEnabled(true);
                                        }
                                );
                            }
                        });
                    } else {
                        //自行实现录音然后往SDK中发送音频数据
                        btnRecord.setText("开始录音");
                        VoiceConvertManager.getInstance().setUseCustomAudioData(true);
                        VoiceConvertManager.getInstance().setWebSocketOnOpen(new WebSocketOpenCallback() {
                            @Override
                            public void onResult(boolean result) {
                                if (result) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(() -> btnRecord.setText("停止录音"));
                                            startRecord();
                                        }
                                    }).start();
                                } else {
                                    Toast.makeText(MainActivity.this, "建立webSocket连接失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }


                } else {
                    isRecording = false;
                    VoiceConvertManager.getInstance().stopRecord();
                    runOnUiThread(() -> {
                        btnRecord.setEnabled(false);
                        btnRecord.setText("正在转换，请稍等");
                    });
                }
            }
        });

        btnFileRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnRecord.setEnabled(false);
                btnFileRecord.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //复制assets文件到内存卡
                            File outputFile = new File(getExternalFilesDir("").getAbsolutePath() + File.separator + "file.pcm");
                            if (outputFile.exists()) {
                                outputFile.delete();
                            }
                            BufferedSink bufferedSink = Okio.buffer(Okio.sink(outputFile));

                            AssetManager assets = getAssets();
                            InputStream inputStream = assets.open("record.pcm");
                            byte[] byteArray = new byte[1024];
                            int len = 0;
                            while ((len = inputStream.read(byteArray)) != -1) {
                                bufferedSink.write(byteArray, 0, len);
                            }
                            bufferedSink.close();
                            VoiceConvertManager.getInstance().startRecordFromFile(outputFile.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
        });

        //设置错误回调
        VoiceConvertManager.getInstance().setErrorCallback(new ErrorCallback() {
            @Override
            public void onError(String errorCode, String errorMessage, String traceId) {
                Utils.log("onError " + errorCode + errorMessage + traceId);
                Toast.makeText(MainActivity.this, errorCode + errorMessage + traceId, Toast.LENGTH_SHORT).show();
            }
        });


        VoiceConvertManager.getInstance().setAudioCallBack(new AudioOutPutCallback() {
            @Override
            public void onAudioOutput(byte[] audioArray, boolean isLast, String traceId) {
                try {
                    if (bufferedSink == null) {
                        File file = new File(mVCFilePath);
                        if (file.exists()) {
                            file.delete();
                        }
                        bufferedSink = Okio.buffer(Okio.sink(file));
                    }
                    bufferedSink.write(audioArray);
                    if (isLast) {
                        bufferedSink.close();
                        bufferedSink = null;
                        startPlay();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *
     */
    private void startPlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnRecord.setText("停止播放");
                btnRecord.setEnabled(true);
            }
        });

        int iMinBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(iMinBufSize)
                .build();
        audioTrack.play();

        try {
            File file = new File(mVCFilePath);
            BufferedSource bufferedSource = Okio.buffer(Okio.source(file));
            byte[] tempByteArray = new byte[1024];
            int len = 0;
            while ((len = bufferedSource.read(tempByteArray)) != -1) {
                audioTrack.write(tempByteArray, 0, len);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "播放完毕", Toast.LENGTH_SHORT).show();
                    btnRecord.setText("开始录音");
                    btnRecord.setEnabled(true);
                    btnFileRecord.setEnabled(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        Utils.log("开始录音");
        isRecording = true;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes * 2);
        audioRecord.startRecording();
        while (isRecording) {
            byte[] tempArray = new byte[10240];
            audioRecord.read(tempArray, 0, tempArray.length);
            VoiceConvertManager.getInstance().sendAudio(tempArray, false);
        }
        VoiceConvertManager.getInstance().sendAudio(new byte[]{}, true);
    }
}