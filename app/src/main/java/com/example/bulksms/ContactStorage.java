package com.example.bulksms;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContactStorage {
    private static final String PREF = "contacts_pref";
    private static final String KEY = "contacts_list";
    private static final Gson GSON = new Gson();

    public static List<Contact> load(Context ctx) {
        String json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Contact>>() {}.getType();
            List<Contact> list = GSON.fromJson(json, type);
            // Guard against null (e.g. the literal "null") and stored entries
            // missing required fields, which would NPE later when sending.
            if (list == null) return new ArrayList<>();
            List<Contact> valid = new ArrayList<>();
            for (Contact c : list) {
                if (c != null && c.getName() != null && c.getPhone() != null) {
                    valid.add(c);
                }
            }
            return valid;
        } catch (Exception e) {
            // Corrupt data — start fresh rather than crashing on launch.
            return new ArrayList<>();
        }
    }

    public static void save(Context ctx, List<Contact> contacts) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, GSON.toJson(contacts)).apply();
    }
}
