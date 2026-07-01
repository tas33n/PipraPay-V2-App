package com.qube.piprapay_tool.Class;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLogger {

    private static final String PREFS_NAME = "LoggerPrefs";
    private static final String KEY_LOGGING_ENABLED = "logging_enabled";
    private static final String LOG_FILE_NAME = "app_debug_logs.txt";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    public static boolean isLoggingEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOGGING_ENABLED, true); // default true for now
    }

    public static void setLoggingEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply();
    }

    public static synchronized void log(Context context, String tag, String message) {
        if (!isLoggingEnabled(context)) {
            Log.d(tag, message);
            return;
        }

        String timeStamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        String logLine = timeStamp + " | " + tag + " | " + message + "\n";
        
        Log.d(tag, message);

        try {
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            
            // Basic log rotation if file gets too big
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                logFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(logLine.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("AppLogger", "Failed to write log: " + e.getMessage());
        }
    }

    public static synchronized String getLogs(Context context) {
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        if (!logFile.exists()) {
            return "No logs found.";
        }

        StringBuilder logs = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                logs.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Error reading logs: " + e.getMessage();
        }

        return logs.toString();
    }

    public static synchronized void clearLogs(Context context) {
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        if (logFile.exists()) {
            logFile.delete();
        }
    }
}
