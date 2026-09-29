package com.seemless;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDbHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String DB_NAME = "medassist_users.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COL_ID      = "_id";
    public static final String COL_NAME    = "name";

    public static final String COL_AGE    = "age";
    public static final String COL_SEX     = "sex";
    public static final String COL_WEIGHT  = "weight";
    public static final String COL_HEIGHT  = "height";
    public static final String COL_BLOOD   = "blood";
    public static final String COL_CONDITIONS = "conditions";
    public static final String COL_MEDICATION    = "medication";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_NAME    + " TEXT NOT NULL," +
                    COL_AGE     + " INTEGER," +
                    COL_SEX     + " TEXT," +
                    COL_WEIGHT  + " REAL," +
                    COL_HEIGHT  + " REAL," +
                    COL_BLOOD   + " TEXT," +
                    COL_CONDITIONS + " TEXT," +
                    COL_MEDICATION    + " TEXT" +
                    ")";

    public UserDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context= context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,      "Anonymous");
        cv.put(COL_AGE,       -1 );
        cv.put(COL_SEX,       "Not specified");
        cv.put(COL_WEIGHT,    -1);
        cv.put(COL_HEIGHT,    -1);
        cv.put(COL_BLOOD,     "Not known");
        cv.put(COL_CONDITIONS, "");
        cv.put(COL_MEDICATION, "");
        long id = db.insert(TABLE_USERS, null, cv);
        SharedPreferences prefs = context.getSharedPreferences("medassist_settings", MODE_PRIVATE);
        prefs.edit().putLong("last_active_user_id", id).apply();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ── INSERT ────────────────────────────────────────────────────
    public long insertUser(String name, int age, String sex, double weight, double height,
                           String blood, String conditions, String medication) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,      name);
        cv.put(COL_AGE,       age );
        cv.put(COL_SEX,       sex);
        cv.put(COL_WEIGHT,    weight);
        cv.put(COL_HEIGHT,    height);
        cv.put(COL_BLOOD,     blood);
        cv.put(COL_CONDITIONS, conditions);
        cv.put(COL_MEDICATION, medication);
        long id = db.insert(TABLE_USERS, null, cv);
        db.close();
        return id;
    }

    // ── UPDATE ────────────────────────────────────────────────────
    public int updateUser(long id, String name, String sex, int age, double weight, double height,
                          String blood, String conditions, String medication) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,         name);
        cv.put(COL_AGE,          age );
        cv.put(COL_SEX,          sex);
        cv.put(COL_WEIGHT,       weight);
        cv.put(COL_HEIGHT,       height);
        cv.put(COL_BLOOD,        blood);
        cv.put(COL_CONDITIONS,   conditions);
        cv.put(COL_MEDICATION,   medication);
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
                c.getInt(c.getColumnIndexOrThrow(COL_AGE)),
                c.getString(c.getColumnIndexOrThrow(COL_SEX)),
                c.getDouble(c.getColumnIndexOrThrow(COL_WEIGHT)),
                c.getDouble(c.getColumnIndexOrThrow(COL_HEIGHT)),
                c.getString(c.getColumnIndexOrThrow(COL_BLOOD)),
                c.getString(c.getColumnIndexOrThrow(COL_CONDITIONS)),
                c.getString(c.getColumnIndexOrThrow(COL_MEDICATION))
        );
    }

    // ── DATA CLASS ────────────────────────────────────────────────
    public static class User {
        public final long    id;
        public final String  name;
        public final int  age;
        public final String  sex;
        public final double  weight;   // kg
        public final double  height;   // cm
        public final String  blood;
        public final String  conditions;
        public final String  medication;

        public User(long id, String name, int age, String sex, double weight,
                    double height, String blood, String conditions,
                    String medication) {
            this.id = id; this.name = name;
            this.age = age;
            this.sex = sex;
            this.weight = weight; this.height = height;
            this.blood = blood; this.conditions = conditions;
            this.medication = medication;
        }

        /** Returns a concise text block suitable for the LLM system prompt. */
        public String toPromptBlock() {
            StringBuilder sb = new StringBuilder();
            sb.append("Patient Profile: ").append(name);
            if (age > 0)                        sb.append(" | Age: ").append(age);
            if (sex != null && !sex.isEmpty())   sb.append(" | Sex: ").append(sex);
            if (weight > 0)                        sb.append(" | Weight: ").append(weight).append(" kg");
            if (height > 0)                        sb.append(" | Height: ").append(height).append(" cm");
            if (blood != null && !blood.isEmpty()) sb.append(" | Blood Type: ").append(blood);
            if (conditions != null && !conditions.isEmpty())
                sb.append(" | Pre-existing Conditions: ").append(conditions);
            if (medication != null && !medication.isEmpty())
                sb.append(" | Current Medication: ").append(medication);
            sb.append("\n\n");
            return sb.toString();
        }
    }
}

