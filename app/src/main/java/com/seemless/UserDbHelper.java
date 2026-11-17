package com.seemless;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class UserDbHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String DB_NAME = "medassist_users.db";
    private static final int DB_VERSION = 3;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID      = "_id";
    public static final String COL_NAME    = "name";
    public static final String COL_DOB     = "dob";
    public static final String COL_SEX     = "sex";
    public static final String COL_WEIGHT  = "weight";
    public static final String COL_HEIGHT  = "height";
    public static final String COL_BLOOD   = "blood";
    public static final String COL_CONDITIONS = "conditions";
    public static final String COL_MEDICATION    = "medication";
    public static final String COL_PULSE           = "pulse";
    public static final String COL_BLOOD_PRESSURE  = "blood_pressure";
    public static final String COL_SMOKING         = "smoking";
    public static final String COL_ALCOHOL         = "alcohol";
    public static final String COL_SPORTS          = "sports";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_ID              + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_NAME            + " TEXT NOT NULL," +
                    COL_DOB             + " INTEGER," +
                    COL_SEX             + " TEXT," +
                    COL_WEIGHT          + " REAL," +
                    COL_HEIGHT          + " REAL," +
                    COL_BLOOD           + " TEXT," +
                    COL_CONDITIONS      + " TEXT," +
                    COL_MEDICATION      + " TEXT," +
                    COL_PULSE           + " INTEGER," +
                    COL_BLOOD_PRESSURE  + " TEXT," +
                    COL_SMOKING         + " TEXT," +
                    COL_ALCOHOL         + " TEXT," +
                    COL_SPORTS          + " TEXT" +
                    ")";

    public UserDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,      "Anonymous");
        cv.put(COL_DOB,       Long.MIN_VALUE);
        cv.put(COL_SEX,       "Not specified");
        cv.put(COL_WEIGHT,    -1);
        cv.put(COL_HEIGHT,    -1);
        cv.put(COL_BLOOD,     "Not specified");
        cv.put(COL_CONDITIONS, "");
        cv.put(COL_MEDICATION, "");
        cv.put(COL_PULSE,           -1);
        cv.put(COL_BLOOD_PRESSURE,  "");
        cv.put(COL_SMOKING,         "");
        cv.put(COL_ALCOHOL,         "");
        cv.put(COL_SPORTS,          "");

        long id = db.insert(TABLE_USERS, null, cv);
        SharedPreferences prefs = context.getSharedPreferences("medassist_settings", MODE_PRIVATE);
        prefs.edit().putLong("last_active_user_id", id).apply();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add new columns if upgrading from v2 or earlier
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_PULSE + " INTEGER DEFAULT -1");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_BLOOD_PRESSURE + " TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_SMOKING + " TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_ALCOHOL + " TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_SPORTS + " TEXT");
            } catch (Exception ignored) {}
        }
    }

    // ── INSERT ────────────────────────────────────────────────────
    public long insertUser(String name, long dob, String sex, double weight, double height,
                           String blood, String conditions, String medication,
                           int pulse, String bloodPressure,
                           String smoking, String alcohol, String sports) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,      name);
        cv.put(COL_DOB,       dob);
        cv.put(COL_SEX,       sex);
        cv.put(COL_WEIGHT,    weight);
        cv.put(COL_HEIGHT,    height);
        cv.put(COL_BLOOD,     blood);
        cv.put(COL_CONDITIONS, conditions);
        cv.put(COL_MEDICATION, medication);
        cv.put(COL_PULSE,           pulse);
        cv.put(COL_BLOOD_PRESSURE,  bloodPressure);
        cv.put(COL_SMOKING,         smoking);
        cv.put(COL_ALCOHOL,         alcohol);
        cv.put(COL_SPORTS,          sports);

        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    // ── UPDATE ────────────────────────────────────────────────────
    public int updateUser(long id, String name, String sex, long dob, double weight, double height,
                          String blood, String conditions, String medication,
                          int pulse, String bloodPressure,
                          String smoking, String alcohol, String sports) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,         name);
        cv.put(COL_DOB,          dob);
        cv.put(COL_SEX,          sex);
        cv.put(COL_WEIGHT,       weight);
        cv.put(COL_HEIGHT,       height);
        cv.put(COL_BLOOD,        blood);
        cv.put(COL_CONDITIONS,   conditions);
        cv.put(COL_MEDICATION,   medication);
        cv.put(COL_PULSE,           pulse);
        cv.put(COL_BLOOD_PRESSURE,  bloodPressure);
        cv.put(COL_SMOKING,         smoking);
        cv.put(COL_ALCOHOL,         alcohol);
        cv.put(COL_SPORTS,          sports);

        int rows = db.update(TABLE_USERS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // ── DELETE ────────────────────────────────────────────────────
    public int deleteUser(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_USERS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // ── SINGLE USER BY ID ─────────────────────────────────────────
    public User getUser(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        User user = null;
        if (c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        db.close();
        return user;
    }

    // ── GET ALL IDS (ordered by name) ─────────────────────────────
    public Cursor getAllIds() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TABLE_USERS,
                new String[]{COL_ID, COL_NAME},
                null, null, null, null, COL_NAME + " ASC");
    }


    // ── HELPER ────────────────────────────────────────────────────
    private User cursorToUser(Cursor c) {
        return new User(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getLong(c.getColumnIndexOrThrow(COL_DOB)),
                c.getString(c.getColumnIndexOrThrow(COL_SEX)),
                c.getDouble(c.getColumnIndexOrThrow(COL_WEIGHT)),
                c.getDouble(c.getColumnIndexOrThrow(COL_HEIGHT)),
                c.getString(c.getColumnIndexOrThrow(COL_BLOOD)),
                c.getString(c.getColumnIndexOrThrow(COL_CONDITIONS)),
                c.getString(c.getColumnIndexOrThrow(COL_MEDICATION)),
                c.getInt(c.getColumnIndexOrThrow(COL_PULSE)),
                c.getString(c.getColumnIndexOrThrow(COL_BLOOD_PRESSURE)),
                c.getString(c.getColumnIndexOrThrow(COL_SMOKING)),
                c.getString(c.getColumnIndexOrThrow(COL_ALCOHOL)),
                c.getString(c.getColumnIndexOrThrow(COL_SPORTS))
        );
    }

    // ── DATA CLASS ────────────────────────────────────────────────
    public static class User {
        public final long    id;
        public final String  name;
        public final long    dob;              // epoch seconds
        public final String  sex;
        public final double  weight;           // kg
        public final double  height;           // cm
        public final String  blood;
        public final String  conditions;
        public final String  medication;
        public final int     pulse;            // bpm, -1 if not set
        public final String  bloodPressure;
        public final String  smoking;
        public final String  alcohol;
        public final String  sports;

        public User(long id, String name, long dob, String sex, double weight,
                    double height, String blood, String conditions,
                    String medication, int pulse, String bloodPressure,
                    String smoking, String alcohol, String sports) {
            this.id = id;
            this.name = name;
            this.dob = dob;
            this.sex = sex;
            this.weight = weight;
            this.height = height;
            this.blood = blood;
            this.conditions = conditions;
            this.medication = medication;
            this.pulse = pulse;
            this.bloodPressure = bloodPressure;
            this.smoking = smoking;
            this.alcohol = alcohol;
            this.sports = sports;
        }

        /** Returns a concise text block suitable for the LLM system prompt. */
        public String toPromptBlock() {
            StringBuilder sb = new StringBuilder();
            sb.append("Patient Profile: ");
            sb.append("Name: ").append(name);

            if (dob > Long.MIN_VALUE) {
                Instant dobInstant = Instant.ofEpochSecond(dob);
                ZoneId zone = ZoneId.systemDefault();
                LocalDate dobDate = dobInstant.atZone(zone).toLocalDate();

                // Calculate precise age (years only)
                LocalDate today = LocalDate.now(zone);
                Period period = Period.between(dobDate, today);
                int age = period.getYears();

                // Format date of birth: "April 25, 1979"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);
                String dobFormatted = dobDate.format(formatter);

                sb.append(" | Date of birth: ").append(dobFormatted).append(" (age ").append(age).append(")");
            }

            if (sex != null && !sex.isEmpty())   sb.append(" | Sex: ").append(sex);
            if (weight > 0)                      sb.append(" | Weight: ").append(weight).append(" kg");
            if (height > 0)                      sb.append(" | Height: ").append(height).append(" cm");
            if (blood != null && !blood.isEmpty()) sb.append(" | Blood Type: ").append(blood);

            StringBuilder vitals = new StringBuilder();
            if (pulse >= 0) {
                vitals.append("Heart: ").append(pulse).append(" bpm");
                if (bloodPressure != null && !bloodPressure.trim().isEmpty()) {
                    vitals.append(", ").append(bloodPressure).append(" mmHg");
                }
            } else if (bloodPressure != null && !bloodPressure.trim().isEmpty()) {
                vitals.append("Heart: ").append(bloodPressure).append(" mmHg");
            }

            if (!vitals.toString().isEmpty()) {
                sb.append(" | Conditions & Vitals: ").append(vitals);
                if (conditions != null && !conditions.trim().isEmpty()) {
                    sb.append(", ").append(conditions);
                }
            } else if (conditions != null && !conditions.isEmpty()) {
                sb.append(" | Conditions & Vitals: ").append(conditions);
            }

            if (medication != null && !medication.isEmpty())
                sb.append(" | Current Medication: ").append(medication);

           StringBuilder lifestyle = new StringBuilder();
            if (smoking != null && !smoking.trim().isEmpty()) {
                lifestyle.append("Smoking: ").append(smoking);
            }
            if (alcohol != null && !alcohol.trim().isEmpty()) {
                if (lifestyle.length() > 0) lifestyle.append(", ");
                lifestyle.append("Alcohol: ").append(alcohol);
            }
            if (sports != null && !sports.trim().isEmpty()) {
                if (lifestyle.length() > 0) lifestyle.append(", ");
                lifestyle.append("Sports: ").append(sports);
            }

            if (lifestyle.length() > 0) {
                sb.append(" | Lifestyle: ").append(lifestyle);
            }

            sb.append("\n\n");
            return sb.toString();
        }
    }
}

