package com.databaker.voiceconvert;

import android.Manifest;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.databaker.voiceconvert.bean.AudioReq;
import com.databaker.voiceconvert.bean.AudioResp;
import com.databaker.voiceconvert.bean.AuthResp;
import com.databaker.voiceconvert.callback.AudioOutPutCallback;
import com.databaker.voiceconvert.callback.AuthCallback;
import com.databaker.voiceconvert.callback.ErrorCallback;
import com.databaker.voiceconvert.callback.SpeechCallback;
import com.databaker.voiceconvert.callback.WebSocketOpenCallback;
import com.databaker.voiceconvert.util.ArrayUtils;
import com.databaker.voiceconvert.util.Utils;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.databaker.voiceconvert.Constants.ERROR_NO_TOKEN;
import static com.databaker.voiceconvert.Constants.ERROR_READ_FILE;
import static com.databaker.voiceconvert.Constants.ERROR_WEB_SOCKET;

public class VoiceConvertManager implements VoiceConvertInterface {
    private static final int SAMPLE_RATE = 16000;

    private String mToken = "";
    private String mVoiceName = "Vc_jiaojiao";
    private boolean enableVad = false;
    private boolean enableAlign = false;
    private boolean enableUseCustomAudioData = false; //自行传入文件流
    private boolean isRecording = false;

    private Context mContext = null;
    private WebSocket mWebSocket = null;
    private ErrorCallback mErrorCallback;
    private AudioOutPutCallback audioOutPutCallback;
    private String mFilePath = ""; // 文件识别路径
    private String mRecordPCMFilePath = ""; // 文件识别路径

    private VoiceConvertManager() {

    }

    private static class Singleton {
        private static final VoiceConvertInterface INSTANCE = new VoiceConvertManager();
    }

    public static VoiceConvertInterface getInstance() {
        return Singleton.INSTANCE;
    }


    @Override
    public void auth(@NotNull Context context, @NotNull String clientId, @NotNull String clientSecret, AuthCallback callback) {
        this.mContext = context;
        if (clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId 不能为空");
        }

        if (clientSecret.isEmpty()) {
            throw new IllegalArgumentException("clientSecret 不能为空");
        }

        OkHttpClient client = new OkHttpClient();
        String url = "https://openapi.data-baker.com/oauth/2.0/token?grant_type=client_credentials&client_secret=" + clientSecret + "&client_id=" + clientId;
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                callback.onResult(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    String json = response.body().string();
                    AuthResp authResp = new Gson().fromJson(json, AuthResp.class);
                    mToken = authResp.getAccess_token();
                    if (mToken != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(true);
                            }
                        });
                        Utils.log("鉴权成功" + mToken);
                    } else {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onResult(false);
                            }
                        });
                        Utils.log("鉴权失败");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    mToken = "";
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(false);
                        }
                    });
                }
            }
        });
    }


    @Override
    public void setVoiceName(String voiceName) {
        mVoiceName = voiceName;
    }

    @Override
    public void setVadEnable(boolean bool) {
        enableVad = bool;
    }

    @Override
    public void setAudioAlign(boolean bool) {
        enableAlign = bool;
    }

    @Override
    public void startRecord(SpeechCallback speechCallback) {

        if (Build.VERSION.SDK_INT > 23) {
            int resultCode = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
            if (resultCode != PERMISSION_GRANTED) {
                throw new RuntimeException("没有申请录音的权限，请申请 Manifest.permission.RECORD_AUDIO 权限");
            }
        }

        if (mToken.isEmpty()) {
            throw new RuntimeException("请先调用 auth 方法授权");
        }

        enableUseCustomAudioData = false;
        mFilePath = "";
        startWebSocket(null, speechCallback);
    }

    @Override
    public void setSaveRecordFile() {
        mRecordPCMFilePath = mContext.getExternalFilesDir("vc").getAbsolutePath() + File.separator + "record.pcm";
    }

    @Override
    public void startRecordFromFile(@NotNull String filePath) {
        if (filePath.isEmpty()) {
            throw new IllegalArgumentException("filePath 不能为空");
        }

        if (mToken.isEmpty()) {
            throw new RuntimeException("请先调用 auth 方法授权");
        }

        enableUseCustomAudioData = false;
        this.mFilePath = filePath;
        startWebSocket(null, null);
    }

    @Override
    public void stopRecord() {
//        if (enableUseCustomAudioData) {
//            throw new IllegalStateException("请不要设置 setReadFile 为 true");
//        }
        isRecording = false;
    }

    @Override
    public void setWebSocketOnOpen(WebSocketOpenCallback openCallback) {
        enableUseCustomAudioData = true;
        startWebSocket(openCallback, null);
    }

    private void startWebSocket(WebSocketOpenCallback openCallback, SpeechCallback speechCallback) {
        if (mWebSocket == null) {
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url("wss://openapi.data-baker.com/ws/voice_conversion").build();
            mWebSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                    try {
                        byte[] resultByteArray = bytes.toByteArray();
                        byte[] prefixArray = Arrays.copyOfRange(resultByteArray, 0, 4);
                        int length = (prefixArray[0] << 24) + (prefixArray[1] << 16) + (prefixArray[2] << 8) + prefixArray[3];
                        if (length > 0) {
                            byte[] jsonArray = Arrays.copyOfRange(resultByteArray, 4, 4 + length);
                            byte[] audioArray = Arrays.copyOfRange(resultByteArray, 4 + length, resultByteArray.length);
                            String jsonStr = new String(jsonArray);
                            AudioResp audioResp = new Gson().fromJson(jsonStr, AudioResp.class);
                            if (audioResp.getErrcode() == 0) {
                                if (audioResp.isLastpkg()) {
                                    audioOutPutCallback.onAudioOutput(audioArray, true, audioResp.getTraceid());
//                                    webSocket.close(1000, "正常关闭");
                                    mWebSocket = null;
                                } else {
                                    audioOutPutCallback.onAudioOutput(audioArray, false, audioResp.getTraceid());
                                }
                            } else {
                                onError(audioResp.getErrcode() + "", audioResp.getErrmsg(), audioResp.getTraceid());
                            }
                        } else {
                            onError(ERROR_WEB_SOCKET, "解析JSON长度出错", "");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        onError(ERROR_WEB_SOCKET, "非法异常" + e.getMessage(), "");
                    }
                }

                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                    onError(ERROR_WEB_SOCKET, t.getMessage(), "");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (enableUseCustomAudioData) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (openCallback != null) {
                                            openCallback.onResult(false);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }

                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                    if (enableUseCustomAudioData) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (openCallback != null) {
                                    openCallback.onResult(true);
                                }
                            }
                        });
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (mFilePath.isEmpty()) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            speechCallback.canSpeech(); // 可以说话
                                        }
                                    });
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
                                    Utils.log("SDK 开始录音");
                                    try {
                                        if (!mRecordPCMFilePath.isEmpty()) {
                                            File file = new File(mRecordPCMFilePath);
                                            if (file.exists()) {
                                                file.delete();
                                            }
                                            BufferedSink bufferedSink = Okio.buffer(Okio.sink(file));
                                            while (isRecording) {
                                                byte[] tempArray = new byte[5120];
                                                audioRecord.read(tempArray, 0, tempArray.length);
                                                packageAudioSend(tempArray, false);
                                                bufferedSink.write(tempArray);
                                            }
                                            bufferedSink.close();
                                            packageAudioSend(new byte[]{}, true);
                                        } else {
                                            while (isRecording) {
                                                byte[] tempArray = new byte[5120];
                                                audioRecord.read(tempArray, 0, tempArray.length);
                                                packageAudioSend(tempArray, false);
                                            }
                                            packageAudioSend(new byte[]{}, true);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        Utils.log("SDK 录音结束");
                                        audioRecord.stop();
                                    }
                                } else {
                                    isRecording = true;
                                    try {
                                        File file = new File(mFilePath);
                                        BufferedSource bufferedSource = Okio.buffer(Okio.source(file));
                                        byte[] readByteArray = bufferedSource.readByteArray();
                                        int numbs = readByteArray.length / 5120;
                                        int remainder = readByteArray.length % 5120;
                                        for (int i = 0; i < numbs; i++) {
                                            if (!isRecording) {
                                                packageAudioSend(new byte[]{}, true);
                                                return;
                                            }
                                            byte[] copyOfRange = Arrays.copyOfRange(readByteArray, i * 5120, (i + 1) * 5120);
                                            if (i == numbs - 1) {
                                                packageAudioSend(copyOfRange, remainder == 0);
                                            } else {
                                                packageAudioSend(copyOfRange, false);
                                            }
                                        }
                                        if (remainder != 0) {
                                            byte[] endArray = Arrays.copyOfRange(readByteArray, numbs * 5120, readByteArray.length);
                                            packageAudioSend(endArray, true);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        onError(ERROR_READ_FILE, "读文件异常" + e.getMessage(), "");
                                    }
                                }
                            }
                        }).start();
                    }
                }
            });
        }

    }

    @Override
    public void sendAudio(byte[] byteArray, boolean isLast) {
        if (mToken.equals("")) {
            onError(ERROR_NO_TOKEN, "无token，请先调用auth()方法进行鉴权", "");
            return;
        }
        if (mWebSocket == null) {
            throw new IllegalStateException("webSocket is null,请先调用 setWebSocketOnOpen 方法建立连接");
        }

        if (mToken.isEmpty()) {
            throw new RuntimeException("请先调用 auth 方法授权");
        }
        enableUseCustomAudioData = true;
        packageAudioSend(byteArray, isLast);
    }

    private void onError(String errorCode, String errorMessage, String traceId) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mErrorCallback.onError(errorCode, errorMessage, traceId);
            }
        });
    }


    private void packageAudioSend(byte[] audioArray, boolean isLast) {
        AudioReq req = new AudioReq(mToken, mVoiceName, enableVad, enableAlign, isLast);
        String json = new Gson().toJson(req);
        byte[] jsonArray = ArrayUtils.toByteArray(json);
        byte[] arrayPrefix = new byte[4];
        arrayPrefix[0] = (byte) (jsonArray.length >> 24 & 0xFF);
        arrayPrefix[1] = (byte) (jsonArray.length >> 16 & 0xFF);
        arrayPrefix[2] = (byte) (jsonArray.length >> 8 & 0xFF);
        arrayPrefix[3] = (byte) (jsonArray.length & 0xFF);
        byte[] resultBA = ArrayUtils.plus(ArrayUtils.plus(arrayPrefix, jsonArray), audioArray);
        mWebSocket.send(new ByteString(resultBA));
    }


    @Override
    public void setAudioCallBack(AudioOutPutCallback callBack) {
        this.audioOutPutCallback = callBack;
    }

    @Override
    public void setErrorCallback(ErrorCallback callback) {
        this.mErrorCallback = callback;
    }

}
