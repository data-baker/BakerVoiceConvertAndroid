package com.databaker.voiceconvert.util;

import android.transition.VisibilityPropagation;
import android.util.Log;

import com.databaker.voiceconvert.VoiceConvertInterface;
import com.databaker.voiceconvert.VoiceConvertManager;

public class Utils {

    public static boolean isPrintLog = true;

    public static void log(String msg) {
        if (isPrintLog) {
            Log.d("dbvclog", msg);
        }
    }

}