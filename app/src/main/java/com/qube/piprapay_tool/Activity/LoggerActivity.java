package com.qube.piprapay_tool.Activity;

import android.os.Bundle;
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
    private ImageView btnClear;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        tvLogs = findViewById(R.id.tvLogs);
        switchLogging = findViewById(R.id.switchLogging);
        btnClear = findViewById(R.id.btnClear);
        btnBack = findViewById(R.id.btnBack);

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
    }

    private void refreshLogs() {
        String logs = AppLogger.getLogs(this);
        if (logs.trim().isEmpty()) {
            tvLogs.setText("No logs yet...");
        } else {
            tvLogs.setText(logs);
        }
    }
}
