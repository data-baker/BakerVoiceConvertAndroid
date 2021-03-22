package com.databaker.voiceconvert.util;

import android.transition.VisibilityPropagation;
import android.util.Log;

public class Utils {

    private static boolean isPrintLog = true;

    public void enableLogPrint(boolean bool) {
        isPrintLog = bool;
    }

    public static void log(String msg) {
        if (isPrintLog) {
            Log.d("dbvclog", msg);
        }
    }

}