package com.qube.piprapay_tool.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.qube.piprapay_tool.Class.SecurityPrefs;
import com.qube.piprapay_tool.R;

public class SecuritySettingsActivity extends AppCompatActivity {

    private EditText etWhitelist;
    private EditText etBlacklist;
    private SwitchCompat switchService;
    private Button btnSave;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_settings);

        etWhitelist = findViewById(R.id.etWhitelist);
        etBlacklist = findViewById(R.id.etBlacklist);
        switchService = findViewById(R.id.switchService);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        // Load existing settings
        etWhitelist.setText(SecurityPrefs.getWhitelist(this));
        etBlacklist.setText(SecurityPrefs.getBlacklist(this));
        switchService.setChecked(SecurityPrefs.isServiceEnabled(this));

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            SecurityPrefs.setWhitelist(this, etWhitelist.getText().toString());
            SecurityPrefs.setBlacklist(this, etBlacklist.getText().toString());
            boolean enableService = switchService.isChecked();
            SecurityPrefs.setServiceEnabled(this, enableService);
            
            if (enableService) {
                Intent serviceIntent = new Intent(this, com.qube.piprapay_tool.Forwarding_Class.SmsReceiverService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                Intent stopIntent = new Intent(this, com.qube.piprapay_tool.Forwarding_Class.SmsReceiverService.class);
                stopIntent.setAction(com.qube.piprapay_tool.Forwarding_Class.SmsReceiverService.ACTION_STOP_SERVICE);
                startService(stopIntent);
            }

            Toast.makeText(this, "Security Settings Saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
