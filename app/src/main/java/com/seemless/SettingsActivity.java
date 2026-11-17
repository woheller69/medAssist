package com.seemless;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SettingsActivity extends AppCompatActivity {

    private UserDbHelper dbHelper;

    private Spinner        spinnerUsers;
    private ArrayAdapter<String> userAdapter;
    private FloatingActionButton btnSave, btnDelete, btnAdd;

    private EditText       etName, etDateOfBirth, etWeight, etHeight, etConditions, etMedication;
    private Spinner        spinnerSex, spinnerBlood;
    private EditText       etPulse;
    private EditText       etBloodPressure;
    private Spinner        spinnerSmoking, spinnerAlcohol;
    private EditText       etSports;

    private long currentUserId = 1; //Anonymous

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ThemeUtils.setStatusBarAppearance(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        dbHelper = new UserDbHelper(this);
        initViews();
        loadUserList();
        setupListeners();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // ── VIEW INIT ────────────────────────────────────────────────
    private void initViews() {
        spinnerUsers = findViewById(R.id.spinnerUsers);
        btnSave      = findViewById(R.id.btnSave);
        btnDelete    = findViewById(R.id.btnDelete);
        btnAdd       = findViewById(R.id.btnAdd);

        etName        = findViewById(R.id.etName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etWeight      = findViewById(R.id.etWeight);
        etHeight      = findViewById(R.id.etHeight);
        etConditions  = findViewById(R.id.etConditions);
        etMedication  = findViewById(R.id.etMedication);

        spinnerSex   = findViewById(R.id.spinnerSex);
        spinnerBlood = findViewById(R.id.spinnerBlood);
        etPulse       = findViewById(R.id.etPulse);
        etBloodPressure = findViewById(R.id.etBloodPressure);
        spinnerSmoking  = findViewById(R.id.spinnerSmoking);
        spinnerAlcohol  = findViewById(R.id.spinnerAlcohol);
        etSports        = findViewById(R.id.etSports);

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

        // Handle Date of Birth
        if (user.dob > Long.MIN_VALUE) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            etDateOfBirth.setText(sdf.format(new Date(user.dob * 1000L))); // dob is in seconds, Date constructor needs milliseconds
        } else {
            etDateOfBirth.setText("");
        }

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

        etPulse.setText(user.pulse != -1 ? String.valueOf(user.pulse) : "");

        etBloodPressure.setText(user.bloodPressure != null ? user.bloodPressure : "");

        String[] smokingDisplay = getResources().getStringArray(R.array.smoking_code_array);
        int smokingPos = findSpinnerIndex(smokingDisplay, user.smoking);
        spinnerSmoking.setSelection(smokingPos);

        String[] alcoholCode    = getResources().getStringArray(R.array.alcohol_code_array);
        int alcoholPos = findSpinnerIndex(alcoholCode, user.alcohol);
        spinnerAlcohol.setSelection(alcoholPos);

        etSports.setText(user.sports != null ? user.sports : "");
    }

    // Helper to find spinner position by matching code string
    private int findSpinnerIndex(String[] displayArray, String value) {
        if (value == null || value.isEmpty()) return 0;
        for (int i = 0; i < displayArray.length; i++) {
            if (value.equals(displayArray[i])) return i;
        }
        return 0; // fallback to first ("Not specified")
    }

    private void clearForm() {
        currentUserId = -1;
        etName.setText("");
        etDateOfBirth.setText("");
        etWeight.setText("");
        etHeight.setText("");
        etConditions.setText("");
        etMedication.setText("");
        etPulse.setText("");
        etBloodPressure.setText("");
        etSports.setText("");
        spinnerSex.setSelection(3);
        spinnerBlood.setSelection(8);
        spinnerSmoking.setSelection(0);
        spinnerAlcohol.setSelection(0);
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

        // ── DATE OF BIRTH CLICK ─────────────────────────────────
        etDateOfBirth.setOnClickListener(v -> {
            long dobSeconds = getEpochFromDateString(etDateOfBirth.getText().toString().trim());
            Calendar c = Calendar.getInstance();
            if (dobSeconds > Long.MIN_VALUE) {
                c.setTimeInMillis(dobSeconds * 1000L);
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    SettingsActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        etDateOfBirth.setText(sdf.format(selectedDate.getTime()));
                    },
                    year, month, day
            );
            dialog.show();
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

            long dob = getEpochFromDateString(etDateOfBirth.getText().toString().trim());
            String sex        = spinnerSex.getSelectedItem().toString();
            double weight     = parseDouble(etWeight.getText().toString(), 0);
            double height     = parseDouble(etHeight.getText().toString(), 0);
            String blood      = spinnerBlood.getSelectedItem().toString();
            String conditions = etConditions.getText().toString().trim();
            String medication = etMedication.getText().toString().trim();

            int pulse = etPulse.getText().toString().equals("") ? -1 : Integer.parseInt(etPulse.getText().toString().trim());

            String bpRaw = etBloodPressure.getText().toString().trim();
            String bpValue = bpRaw.equals("") ? "" : validateBP(bpRaw);

            String[] smokingCode = getResources().getStringArray(R.array.smoking_code_array);
            String[] alcoholCode = getResources().getStringArray(R.array.alcohol_code_array);

            String smokingValue = smokingCode[spinnerSmoking.getSelectedItemPosition()];
            String alcoholValue = alcoholCode[spinnerAlcohol.getSelectedItemPosition()];

            String sportsValue = etSports.getText().toString().trim();

            UserDbHelper.User existing = getUserByName(name);

            if (existing != null && existing.id == currentUserId) {
                dbHelper.updateUser(currentUserId, name, sex, dob, weight, height, blood, conditions, medication,
                        pulse, bpValue, smokingValue, alcoholValue, sportsValue);
                Toast.makeText(this, "Patient updated", Toast.LENGTH_SHORT).show();
            } else if (existing != null && existing.id != currentUserId) {
                dbHelper.updateUser(existing.id, name, sex, dob, weight, height, blood, conditions, medication,
                        pulse, bpValue, smokingValue, alcoholValue, sportsValue);
                currentUserId = existing.id;
                Toast.makeText(this, "Patient \"" + name + "\" updated (merged)", Toast.LENGTH_SHORT).show();
            } else {
                long id = dbHelper.insertUser(name, dob, sex, weight, height, blood, conditions, medication,
                        pulse, bpValue, smokingValue, alcoholValue, sportsValue);
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
    private double parseDouble(String s, double defaultVal) {
        try { return Double.parseDouble(s.isEmpty() ? "0" : s); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private long getEpochFromDateString(String dateString) {
        // Handle empty date field
        if (dateString == null || dateString.trim().isEmpty()) {
            // Return Long.MIN_VALUE to indicate "unknown date of birth"
            return Long.MIN_VALUE;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateString);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(date);
            return cal.getTimeInMillis() / 1000;
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error parsing date: " + dateString, e);
            // Return Long.MIN_VALUE to indicate "unknown date of birth"
            return Long.MIN_VALUE;
        }
    }

    // ✅ Validate BP format: "X/Y" (e.g. "120/80")
    private String validateBP(String bp) {
        if (bp == null || bp.isEmpty()) return "";
        String[] parts = bp.split("/");
        if (parts.length == 2) {
            try {
                int sys = Integer.parseInt(parts[0].trim());
                int dia = Integer.parseInt(parts[1].trim());
                if (sys > 0 && dia > 0) return sys + "/" + dia;
            } catch (NumberFormatException ignored) {}
        }
        Toast.makeText(this, "Invalid BP format. Use '120/80'", Toast.LENGTH_SHORT).show();
        return "";
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
