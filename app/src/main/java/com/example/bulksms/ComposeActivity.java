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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class ComposeActivity extends AppCompatActivity {

    private String[] numbers;
    private TextInputEditText editMessage;
    private Button btnSend;

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
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compose);

        // Push content below status bar and above keyboard/nav bar.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.composeRoot), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, bars.top, bars.right,
                            Math.max(bars.bottom, ime.bottom));
                    return insets;
                });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Compose SMS");
        }

        numbers = getIntent().getStringArrayExtra("numbers");
        if (numbers == null) numbers = new String[0];

        TextView textRecipients = findViewById(R.id.textRecipients);
        textRecipients.setText(numbers.length + " recipient(s):\n" + TextUtils.join(", ", numbers));
        // Cap the list height (set in XML) and let it scroll, so a large recipient
        // list can't squeeze the message box down to nothing.
        textRecipients.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        editMessage = findViewById(R.id.editMessage);

        btnSend = findViewById(R.id.btnSend);
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
        if (numbers.length == 0) {
            Toast.makeText(this, "No recipients", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = getSystemService(SmsManager.class);
            if (smsManager == null) smsManager = SmsManager.getDefault();
        } else {
            smsManager = SmsManager.getDefault();
        }
        final SmsManager sms = smsManager;

        // Disable the button so a second tap can't start a parallel send.
        btnSend.setEnabled(false);
        btnSend.setText("Sending…");

        // divideMessage splits messages longer than a single SMS (160 chars, or 70 for
        // unicode) into parts so nothing is silently truncated. Same message for everyone,
        // so compute it once outside the loop.
        java.util.ArrayList<String> parts = sms.divideMessage(message);
        boolean multipart = parts.size() > 1;

        // Send off the main thread — a large recipient list would otherwise freeze the UI.
        new Thread(() -> {
            int sent = 0;
            java.util.List<String> failed = new java.util.ArrayList<>();
            for (String number : numbers) {
                try {
                    if (multipart) {
                        sms.sendMultipartTextMessage(number, null, parts, null, null);
                    } else {
                        sms.sendTextMessage(number, null, message, null, null);
                    }
                    sent++;
                } catch (Exception e) {
                    failed.add(number);
                }
            }
            final int sentCount = sent;
            final java.util.List<String> failedNumbers = failed;
            runOnUiThread(() -> showSendResult(sentCount, failedNumbers));
        }).start();
    }

    private void showSendResult(int sentCount, java.util.List<String> failedNumbers) {
        if (failedNumbers.isEmpty()) {
            Toast.makeText(this, "Sent to " + sentCount + " recipient(s)",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Some sends failed — list the numbers so the user knows exactly which to retry.
        new AlertDialog.Builder(this)
                .setTitle("Sent to " + sentCount + ", " + failedNumbers.size() + " failed")
                .setMessage("These numbers failed:\n\n" + TextUtils.join("\n", failedNumbers))
                .setPositiveButton("OK", (d, w) -> {
                    if (sentCount > 0) {
                        finish();
                    } else {
                        // Nothing sent — keep the screen so the message can be retried.
                        btnSend.setEnabled(true);
                        btnSend.setText("Send SMS");
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
