package com.example.bulksms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private List<Contact> contacts;
    private ContactAdapter adapter;
    private Button btnCompose;
    private TextView emptyView;

    // ── Activity result launchers (must be registered before onCreate) ──────────

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    contacts.add(new Contact(
                            data.getStringExtra("name"),
                            data.getStringExtra("phone")));
                    adapter.refreshFilter();
                    ContactStorage.save(this, contacts);
                }
            });

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int pos = data.getIntExtra("position", -1);
                    if (pos >= 0 && pos < contacts.size()) {
                        contacts.get(pos).setName(data.getStringExtra("name"));
                        contacts.get(pos).setPhone(data.getStringExtra("phone"));
                        adapter.refreshFilter();
                        ContactStorage.save(this, contacts);
                    }
                }
            });

    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    doImport();
                } else {
                    Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Push bottom buttons above the keyboard when it appears.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.coordinatorLayout), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, 0, bars.right,
                            Math.max(bars.bottom, ime.bottom));
                    return insets;
                });

        emptyView = findViewById(R.id.emptyView);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // Do NOT call setSupportActionBar() here — it takes ownership of the toolbar's
        // menu system and wipes the items already loaded via app:menu in XML.
        // MainActivity needs no ActionBar APIs (no back button), so we drive the
        // toolbar directly.
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                        toolbar, com.google.android.material.R.attr.colorOnSurface));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import) {
                handleImport();
                return true;
            }
            return false;
        });

        contacts = ContactStorage.load(this);
        adapter = new ContactAdapter(contacts, new ContactAdapter.Listener() {
            @Override
            public void onSelectionChanged(int count) {
                updateComposeButton(count);
            }

            @Override
            public void onLongPress(int position) {
                showContactOptions(position);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupSwipeToDelete(recyclerView);

        // Dismiss keyboard whenever the user touches the contact list.
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv,
                    @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) hideKeyboard();
                return false;
            }
        });

        // Search
        TextInputEditText editSearch = findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
            }
        });

        // Select All (filters to currently visible contacts)
        Button btnSelectAll = findViewById(R.id.btnSelectAll);
        btnSelectAll.setOnClickListener(v -> adapter.selectAllFiltered());

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v ->
                addLauncher.launch(new Intent(this, AddEditContactActivity.class)));

        btnCompose = findViewById(R.id.btnCompose);
        btnCompose.setOnClickListener(v -> {
            List<Contact> sel = adapter.getSelectedContacts();
            if (sel.isEmpty()) {
                Toast.makeText(this, "Select at least one contact", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] numbers = new String[sel.size()];
            for (int i = 0; i < sel.size(); i++) numbers[i] = sel.get(i).getPhone();
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra("numbers", numbers);
            startActivity(intent);
            adapter.clearSelection();
        });
    }

    // ── Import from phone contacts ───────────────────────────────────────────────

    private void handleImport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            doImport();
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void doImport() {
        List<Contact> phoneContacts = readPhoneContacts();
        if (phoneContacts.isEmpty()) {
            Toast.makeText(this, "No contacts found on device", Toast.LENGTH_SHORT).show();
            return;
        }
        showImportDialog(phoneContacts);
    }

    private List<Contact> readPhoneContacts() {
        List<Contact> result = new ArrayList<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            int nameCol = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneCol = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameCol);
                String phone = cursor.getString(phoneCol);
                if (name != null && phone != null
                        && !name.trim().isEmpty() && !phone.trim().isEmpty()) {
                    result.add(new Contact(name.trim(), phone.trim()));
                }
            }
            cursor.close();
        }
        return result;
    }

    private void showImportDialog(List<Contact> phoneContacts) {
        Set<String> existing = getExistingPhones();

        // Filter out contacts whose numbers are already in the app, and de-duplicate
        // within the phone's own list (the address book often has the same number twice).
        List<Contact> importable = new ArrayList<>();
        Set<String> seen = new HashSet<>(existing);
        for (Contact c : phoneContacts) {
            String norm = normalizePhone(c.getPhone());
            if (!norm.isEmpty() && seen.add(norm)) {
                importable.add(c);
            }
        }

        if (importable.isEmpty()) {
            Toast.makeText(this, "All contacts are already imported", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[importable.size()];
        boolean[] checked = new boolean[importable.size()];
        Arrays.fill(checked, true);
        for (int i = 0; i < importable.size(); i++) {
            labels[i] = importable.get(i).getName() + "\n" + importable.get(i).getPhone();
        }

        new AlertDialog.Builder(this)
                .setTitle("Import Contacts (" + importable.size() + " new)")
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Import", (d, w) -> {
                    int count = 0;
                    for (int i = 0; i < importable.size(); i++) {
                        if (checked[i]) {
                            contacts.add(importable.get(i));
                            count++;
                        }
                    }
                    if (count > 0) {
                        adapter.refreshFilter();
                        ContactStorage.save(this, contacts);
                        Toast.makeText(this,
                                "Imported " + count + " contact(s)", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Set<String> getExistingPhones() {
        Set<String> phones = new HashSet<>();
        for (Contact c : contacts) {
            String norm = normalizePhone(c.getPhone());
            if (!norm.isEmpty()) phones.add(norm);
        }
        return phones;
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    // ── Swipe to delete ──────────────────────────────────────────────────────────

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                    @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                Contact c = adapter.getItem(pos);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Contact")
                        .setMessage("Delete " + c.getName() + "?")
                        .setPositiveButton("Delete", (d, w) -> {
                            contacts.remove(c);
                            adapter.clearSelection();
                            adapter.refreshFilter();
                            ContactStorage.save(MainActivity.this, contacts);
                        })
                        .setNegativeButton("Cancel", (d, w) -> adapter.refreshFilter())
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    // ── Contact options (long-press) ─────────────────────────────────────────────

    private void showContactOptions(int filteredPosition) {
        Contact c = adapter.getItem(filteredPosition);
        int masterPos = contacts.indexOf(c);
        new AlertDialog.Builder(this)
                .setTitle(c.getName())
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, AddEditContactActivity.class);
                        intent.putExtra("name", c.getName());
                        intent.putExtra("phone", c.getPhone());
                        intent.putExtra("position", masterPos);
                        editLauncher.launch(intent);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Delete Contact")
                                .setMessage("Delete " + c.getName() + "?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    contacts.remove(c);
                                    adapter.clearSelection();
                                    adapter.refreshFilter();
                                    ContactStorage.save(this, contacts);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void updateComposeButton(int count) {
        if (count == 0) {
            btnCompose.setText(R.string.compose_sms);
        } else {
            btnCompose.setText(getString(R.string.compose_sms) + " (" + count + ")");
        }
        if (adapter.getItemCount() == 0) {
            // Distinguish "no contacts at all" from "search returned nothing".
            emptyView.setText(contacts.isEmpty()
                    ? R.string.empty_contacts : R.string.empty_search);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }
}
