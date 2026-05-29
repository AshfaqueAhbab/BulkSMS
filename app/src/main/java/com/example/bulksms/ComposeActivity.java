package com.example.bulksms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class ComposeActivity extends AppCompatActivity {

    private String[] numbers;
    private TextInputEditText editMessage;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    sendSms();
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Compose SMS");
        }

        numbers = getIntent().getStringArrayExtra("numbers");
        if (numbers == null) numbers = new String[0];

        TextView textRecipients = findViewById(R.id.textRecipients);
        textRecipients.setText(TextUtils.join(", ", numbers));

        editMessage = findViewById(R.id.editMessage);

        Button btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                permissionLauncher.launch(Manifest.permission.SEND_SMS);
            }
        });
    }

    private void sendSms() {
        String message = editMessage.getText() != null
                ? editMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(message)) {
            editMessage.setError("Message cannot be empty");
            return;
        }

        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = getSystemService(SmsManager.class);
            if (smsManager == null) smsManager = SmsManager.getDefault();
        } else {
            smsManager = SmsManager.getDefault();
        }

        int sent = 0;
        for (String number : numbers) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null);
                sent++;
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send to " + number, Toast.LENGTH_SHORT).show();
            }
        }

        if (sent > 0) {
            Toast.makeText(this, "Sent to " + sent + " recipient(s)", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
