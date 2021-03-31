package com.databaker.voiceconvert.bean;

public class AudioResp {

    /**
     * 错误码
     */
    private int errcode;
    /**
     * 错误信息
     */
    private String errmsg;
    /**
     * 本次音频转换的唯一ID
     */
    private String traceid;
    /**
     * 是否是最后一包
     */
    private boolean lastpkg;


    @Override
    public String toString() {
        return "AudioResp{" +
                "errcode=" + errcode +
                ", errmsg='" + errmsg + '\'' +
                ", traceid='" + traceid + '\'' +
                ", lastpkg=" + lastpkg +
                '}';
    }

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public String getTraceid() {
        return traceid;
    }

    public void setTraceid(String traceid) {
        this.traceid = traceid;
    }

    public boolean isLastpkg() {
        return lastpkg;
    }

    public void setLastpkg(boolean lastpkg) {
        this.lastpkg = lastpkg;
    }
}
