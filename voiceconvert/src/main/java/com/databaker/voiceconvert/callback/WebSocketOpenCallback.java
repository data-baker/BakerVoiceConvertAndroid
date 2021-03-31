package com.databaker.voiceconvert.callback;

public interface WebSocketOpenCallback {

    /**
     * 主线程回调
     * @param result
     */
    void onResult(boolean result);

}
