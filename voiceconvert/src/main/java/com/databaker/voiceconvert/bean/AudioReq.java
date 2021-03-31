package com.databaker.voiceconvert.bean;

public class AudioReq {

    private String access_token;

    /**
     * 音色名字
     */
    private String voice_name;
    /**
     * 是否开启vad
     */
    private boolean enable_vad;
    /**
     * 是否开启音频对齐
     */
    private boolean align_input;
    /**
     * 是否为最后一包
     */
    private boolean lastpkg;

    public AudioReq(String access_token, String voice_name, boolean enable_vad, boolean align_input, boolean lastpkg) {
        this.access_token = access_token;
        this.voice_name = voice_name;
        this.enable_vad = enable_vad;
        this.align_input = align_input;
        this.lastpkg = lastpkg;
    }

    @Override
    public String toString() {
        return "AudioReq{" +
                "access_token='" + access_token + '\'' +
                ", voice_name='" + voice_name + '\'' +
                ", enable_vad=" + enable_vad +
                ", align_input=" + align_input +
                ", lastpkg=" + lastpkg +
                '}';
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getVoice_name() {
        return voice_name;
    }

    public void setVoice_name(String voice_name) {
        this.voice_name = voice_name;
    }

    public boolean isEnable_vad() {
        return enable_vad;
    }

    public void setEnable_vad(boolean enable_vad) {
        this.enable_vad = enable_vad;
    }

    public boolean isAlign_input() {
        return align_input;
    }

    public void setAlign_input(boolean align_input) {
        this.align_input = align_input;
    }

    public boolean isLastpkg() {
        return lastpkg;
    }

    public void setLastpkg(boolean lastpkg) {
        this.lastpkg = lastpkg;
    }
}
