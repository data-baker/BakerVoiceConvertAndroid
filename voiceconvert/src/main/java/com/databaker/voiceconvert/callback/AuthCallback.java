package com.databaker.voiceconvert.callback;

public interface AuthCallback {

    /**
     * 主线程中回调
     *
     * @param result
     */
    void onResult(boolean result);
}
