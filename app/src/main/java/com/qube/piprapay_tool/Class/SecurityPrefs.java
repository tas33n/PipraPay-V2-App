package com.qube.piprapay_tool.Class;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

public class SecurityPrefs {
    private static final String PREFS_NAME = "SecurityPrefs";
    private static final String KEY_WHITELIST = "whitelist";
    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_SERVICE = "foreground_service_enabled";

    // Default: allow all
    private static final String DEFAULT_WHITELIST = "*";
    // Default: block OTP related words
    private static final String DEFAULT_BLACKLIST = "otp,pin,secret,password,verification";

    public static String getWhitelist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WHITELIST, DEFAULT_WHITELIST);
    }

    public static void setWhitelist(Context context, String whitelist) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WHITELIST, whitelist).apply();
    }

    public static String getBlacklist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BLACKLIST, DEFAULT_BLACKLIST);
    }

    public static void setBlacklist(Context context, String blacklist) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BLACKLIST, blacklist).apply();
    }

    public static boolean isServiceEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SERVICE, true);
    }

    public static void setServiceEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SERVICE, enabled).apply();
    }

    // Helper methods for SmsBroadcastReceiver
    public static boolean isSenderWhitelisted(Context context, String sender) {
        String whitelist = getWhitelist(context);
        if (whitelist.contains("*")) return true;
        
        if (sender == null) return false;
        
        List<String> allowed = Arrays.asList(whitelist.split(","));
        for (String s : allowed) {
            if (sender.trim().equalsIgnoreCase(s.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsBlacklistedKeyword(Context context, String message) {
        if (message == null) return false;
        
        String blacklist = getBlacklist(context);
        if (blacklist.trim().isEmpty()) return false;
        
        String lowerMessage = message.toLowerCase();
        List<String> blocked = Arrays.asList(blacklist.split(","));
        for (String b : blocked) {
            if (!b.trim().isEmpty() && lowerMessage.contains(b.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
