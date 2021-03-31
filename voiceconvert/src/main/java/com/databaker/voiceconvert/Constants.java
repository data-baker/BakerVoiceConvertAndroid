package com.databaker.voiceconvert;

public class Constants {

    private static final String ERROR_CODE_PREFIX = "191";

    /**
     * 没有token，先进行鉴权
     */
    public static final String ERROR_NO_TOKEN = ERROR_CODE_PREFIX + "00001";


    /**
     * 网络请求出错
     */
    public static final String ERROR_WEB_SOCKET = ERROR_CODE_PREFIX + "00002";

    /**
     * 读文件异常
     */
    public static final String ERROR_READ_FILE = ERROR_CODE_PREFIX + "00003";
}
