package com.qube.piprapay_tool.Activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.qube.piprapay_tool.Class.AppLogger;
import com.qube.piprapay_tool.R;

public class LoggerActivity extends AppCompatActivity {

    private TextView tvLogs;
    private SwitchCompat switchLogging;
    private ImageView btnBack, btnClear, btnCopy, btnShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        tvLogs = findViewById(R.id.tvLogs);
        switchLogging = findViewById(R.id.switchLogging);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);

        // Initialize state
        switchLogging.setChecked(AppLogger.isLoggingEnabled(this));
        refreshLogs();

        // Listeners
        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            AppLogger.clearLogs(this);
            refreshLogs();
            Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
        });

        switchLogging.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppLogger.setLoggingEnabled(this, isChecked);
            String state = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Logging " + state, Toast.LENGTH_SHORT).show();
            if (isChecked) {
                AppLogger.log(this, "LoggerActivity", "Logging enabled");
                refreshLogs();
            }
        });
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("App Logs", tvLogs.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            String logs = tvLogs.getText().toString();
            if (logs.length() > 100000) {
                logs = logs.substring(logs.length() - 100000); // Limit to last 100k chars for intent limit
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "PipraPay Debug Logs");
            intent.putExtra(Intent.EXTRA_TEXT, logs);
            startActivity(Intent.createChooser(intent, "Share logs via"));
        });
    }

    private void refreshLogs() {
        String rawLogs = AppLogger.getLogs(this);
        if (rawLogs.trim().isEmpty() || rawLogs.equals("No logs yet...")) {
            tvLogs.setText("No logs yet...");
            return;
        }

        // Parse plain text logs and apply HTML colors for UI only
        StringBuilder htmlBuilder = new StringBuilder();
        String[] lines = rawLogs.split("\n");
        for (String line : lines) {
            String color = "#333333"; // Default DEBUG / V
            if (line.contains(" E/")) {
                color = "#D32F2F";
            } else if (line.contains(" W/")) {
                color = "#F57C00";
            } else if (line.contains(" I/")) {
                color = "#1976D2";
            }

            // Escape HTML in the line so that raw XML/HTML in messages isn't parsed as tags
            String escapedLine = android.text.TextUtils.htmlEncode(line);
            
            htmlBuilder.append("<font color='").append(color).append("'>")
                       .append(escapedLine)
                       .append("</font><br/>");
        }

        tvLogs.setText(Html.fromHtml(htmlBuilder.toString(), Html.FROM_HTML_MODE_LEGACY));
    }
}
