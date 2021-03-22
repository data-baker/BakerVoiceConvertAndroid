package com.databaker.voiceconvert.callback;

public interface ErrorCallback {

    /**
     * 错误回调
     *
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @param traceId      如果发送错误，反馈的时候请提供traceId
     */
    void onError(String errorCode, String errorMessage, String traceId);

}