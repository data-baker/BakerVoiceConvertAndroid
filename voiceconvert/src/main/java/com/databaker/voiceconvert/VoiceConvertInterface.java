package com.databaker.voiceconvert;

import android.content.Context;

import com.databaker.voiceconvert.callback.AudioOutPutCallback;
import com.databaker.voiceconvert.callback.AuthCallback;
import com.databaker.voiceconvert.callback.ErrorCallback;
import com.databaker.voiceconvert.callback.SpeechCallback;
import com.databaker.voiceconvert.callback.WebSocketOpenCallback;

public interface VoiceConvertInterface {

    /**
     * 鉴权接口
     */
    void auth(Context context, String clientId, String clientSecret, AuthCallback callback);

    /**
     * 设置音色
     */
    void setVoiceName(String voiceName);

    /**
     * 设置vad是否开启
     */
    void setVadEnable(boolean bool);

    /**
     * 设置音频对齐
     */
    void setAudioAlign(boolean bool);


    /**
     * 开始录音，自动请求
     */
    void startRecord(SpeechCallback speechCallback);

    /**
     * 保存录音的原始文件，只有在调用startRecord方法的时候起作用，使用别的方法进行声音转换此方法不起作用
     */
    void setSaveRecordFile();


    /**
     * 直接传入文件进行声音转换
     */
    void startRecordFromFile(String filePath);


    /**
     * 停止录音
     */
    void stopRecord();

    /**
     * 设置使用自定义音频数据
     *
     * @param bool
     */
    void setUseCustomAudioData(boolean bool);

    /**
     * 设置websocket的onOpen方法回调，只有返回结果成功后才能调用 sendAudio 方法
     *
     * @param openCallback
     */
    void setWebSocketOnOpen(WebSocketOpenCallback openCallback);

    /**
     * 发送音频
     *
     * @param byteArray 音频序列
     * @param isLast    是否是最后一包
     */
    void sendAudio(byte[] byteArray, boolean isLast);

    /**
     * 设置变声处理后的音频序列回调
     *
     * @param callBack
     */
    void setAudioCallBack(AudioOutPutCallback callBack);

    /**
     * 设置全局错误回调
     */
    void setErrorCallback(ErrorCallback callback);

}
