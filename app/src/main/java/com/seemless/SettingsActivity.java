package com.seemless;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SettingsActivity extends AppCompatActivity {

    private UserDbHelper dbHelper;

    private Spinner        spinnerUsers;
    private ArrayAdapter<String> userAdapter;
    private FloatingActionButton btnSave, btnDelete, btnAdd;

    private EditText       etName, etAge, etWeight, etHeight, etConditions, etMedication;
    private Spinner        spinnerSex, spinnerBlood;

    private long currentUserId = 1; //Anonymous

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new UserDbHelper(this);
        initViews();
        loadUserList();
        setupListeners();
    }

    // ── VIEW INIT ────────────────────────────────────────────────
// ── Load last-active on create (restore spinner position) ────
    private void initViews() {
        spinnerUsers = findViewById(R.id.spinnerUsers);
        btnSave      = findViewById(R.id.btnSave);
        btnDelete    = findViewById(R.id.btnDelete);
        btnAdd       = findViewById(R.id.btnAdd);

        etName        = findViewById(R.id.etName);
        etAge         = findViewById(R.id.etAge);
        etWeight      = findViewById(R.id.etWeight);
        etHeight      = findViewById(R.id.etHeight);
        etConditions  = findViewById(R.id.etConditions);
        etMedication  = findViewById(R.id.etMedication);

        spinnerSex   = findViewById(R.id.spinnerSex);
        spinnerBlood = findViewById(R.id.spinnerBlood);

        // Restore last-active user so spinner lands on the right one
        SharedPreferences prefs = getSharedPreferences("medassist_settings", MODE_PRIVATE);
        long lastActiveId = prefs.getLong("last_active_user_id", 1L);
        if (lastActiveId != 1L) {
            UserDbHelper.User u = dbHelper.getUser(lastActiveId);
            if (u != null) {
                currentUserId = u.id;
                loadUserForm(u);
            }
        }
    }


    // ── POPULATE USER SPINNER ───────────────────────────────────
    private void loadUserList() {
        userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        refreshUserList();
    }

    private void refreshUserList() {
        userAdapter.clear();
        userAdapter.add("— Select patient —");

        android.database.Cursor c = dbHelper.getAllIds();
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow(UserDbHelper.COL_NAME));
            userAdapter.add(name);
        }
        c.close();
        spinnerUsers.setAdapter(userAdapter);

        // currentUserId was already set in initViews() if one exists
        for (int i = 0; i < userAdapter.getCount(); i++) {
            String n = userAdapter.getItem(i);
            if (n == null) continue;
            UserDbHelper.User u = getUserByName(n);
            if (u != null && u.id == currentUserId) {
                spinnerUsers.setSelection(i);
                break;
            }
        }

    }

    private UserDbHelper.User getUserByName(String name) {
        android.database.Cursor c = dbHelper.getAllIds();
        while (c.moveToNext()) {
            long id = c.getLong(c.getColumnIndexOrThrow(UserDbHelper.COL_ID));
            String n = c.getString(c.getColumnIndexOrThrow(UserDbHelper.COL_NAME));
            if (n.equals(name)) {
                UserDbHelper.User u = dbHelper.getUser(id);
                c.close();
                return u;
            }
        }
        c.close();
        return null;
    }

    // ── LOAD / CLEAR FORM ───────────────────────────────────────
    private void loadUserForm(UserDbHelper.User user) {
        currentUserId = user.id;
        etName.setText(user.name);
        if (user.age > 0)          etAge.setText(String.valueOf(user.age));
        else                       etAge.setText("");
        if (user.weight > 0)       etWeight.setText(String.valueOf(user.weight));
        else                       etWeight.setText("");
        if (user.height > 0)       etHeight.setText(String.valueOf(user.height));
        else                       etHeight.setText("");

        if (user.sex != null) {
            String[] sexes = getResources().getStringArray(R.array.sex_array);
            int pos = java.util.Arrays.asList(sexes).indexOf(user.sex);
            spinnerSex.setSelection(pos >= 0 ? pos : 0);
        }
        if (user.blood != null) {
            String[] bloods = getResources().getStringArray(R.array.blood_array);
            int pos = java.util.Arrays.asList(bloods).indexOf(user.blood);
            spinnerBlood.setSelection(pos >= 0 ? pos : 0);
        }

        etConditions.setText(user.conditions != null ? user.conditions : "");
        etMedication.setText(user.medication != null ? user.medication : "");
    }

    private void clearForm() {
        currentUserId = -1;
        etName.setText("");
        etAge.setText("");
        etWeight.setText("");
        etHeight.setText("");
        etConditions.setText("");
        etMedication.setText("");
        spinnerSex.setSelection(3);
        spinnerBlood.setSelection(8);
    }

    // ── LISTENERS ───────────────────────────────────────────────
    private void setupListeners() {

        spinnerUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) { clearForm(); return; }
                String name = parent.getItemAtPosition(pos).toString();
                UserDbHelper.User u = getUserByName(name);
                if (u != null) loadUserForm(u);
                persistActiveUser();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── ADD BUTTON ───────────────────────────────────────
        btnAdd.setOnClickListener(v -> {
            clearForm();
        });
        // ── SAVE BUTTON ───────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a patient name", Toast.LENGTH_SHORT).show();
                return;
            }

            int    age        = parseInt(etAge.getText().toString(), 0);
            String sex        = spinnerSex.getSelectedItem().toString();
            double weight     = parseDouble(etWeight.getText().toString(), 0);
            double height     = parseDouble(etHeight.getText().toString(), 0);
            String blood      = spinnerBlood.getSelectedItem().toString();
            String conditions = etConditions.getText().toString().trim();
            String medication = etMedication.getText().toString().trim();

            UserDbHelper.User existing = getUserByName(name);

            if (existing != null && existing.id == currentUserId) {
                dbHelper.updateUser(currentUserId, name, sex, age, weight, height, blood, conditions, medication);
                Toast.makeText(this, "Patient updated", Toast.LENGTH_SHORT).show();
            } else if (existing != null && existing.id != currentUserId) {
                dbHelper.updateUser(existing.id, name, sex, age, weight, height, blood, conditions, medication);
                currentUserId = existing.id;
                Toast.makeText(this, "Patient \"" + name + "\" updated (merged)", Toast.LENGTH_SHORT).show();
            } else {
                long id = dbHelper.insertUser(name, age, sex, weight, height, blood, conditions, medication);
                if (id != -1) {
                    currentUserId = id;
                    Toast.makeText(this, "Patient \"" + name + "\" added", Toast.LENGTH_SHORT).show();
                }
            }
            persistActiveUser();
            refreshUserList();
        });


        // ── DELETE BUTTON ───────────────────────────────────────
        btnDelete.setOnClickListener(v -> {
            if (currentUserId == 1) return;
            String name = etName.getText().toString().trim();
            new AlertDialog.Builder(this)
                    .setMessage("Delete patient \"" + name + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        dbHelper.deleteUser(currentUserId);
                        clearForm();
                        refreshUserList();
                        loadUserList();
                        Toast.makeText(SettingsActivity.this, "Patient deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ── HELPERS ─────────────────────────────────────────────────
    private int parseInt(String s, int defaultVal) {
        try { return Integer.parseInt(s.isEmpty() ? "0" : s); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private double parseDouble(String s, double defaultVal) {
        try { return Double.parseDouble(s.isEmpty() ? "0" : s); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private void persistActiveUser() {
        if (currentUserId != -1) {
            getSharedPreferences("medassist_settings", MODE_PRIVATE)
                    .edit().putLong("last_active_user_id", currentUserId).apply();
        }
    }
}
