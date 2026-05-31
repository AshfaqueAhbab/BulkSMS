package com.example.bulksms;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddEditContactActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextInputLayout layoutName = findViewById(R.id.layoutName);
        TextInputLayout layoutPhone = findViewById(R.id.layoutPhone);
        TextInputEditText editName = findViewById(R.id.editName);
        TextInputEditText editPhone = findViewById(R.id.editPhone);

        Intent incoming = getIntent();
        int position = incoming.getIntExtra("position", -1);
        boolean isEditing = position >= 0;

        setTitle(isEditing ? "Edit Contact" : "Add Contact");
        if (isEditing) {
            editName.setText(incoming.getStringExtra("name"));
            editPhone.setText(incoming.getStringExtra("phone"));
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";

            boolean valid = true;
            if (TextUtils.isEmpty(name)) {
                layoutName.setError("Name is required");
                valid = false;
            } else {
                layoutName.setError(null);
            }
            if (TextUtils.isEmpty(phone)) {
                layoutPhone.setError("Phone is required");
                valid = false;
            } else if (phone.replaceAll("[^0-9]", "").length() < 3) {
                // A real number needs digits — reject "abc" or stray symbols.
                layoutPhone.setError("Enter a valid phone number");
                valid = false;
            } else {
                layoutPhone.setError(null);
            }
            if (!valid) return;

            Intent result = new Intent();
            result.putExtra("name", name);
            result.putExtra("phone", phone);
            if (isEditing) result.putExtra("position", position);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
