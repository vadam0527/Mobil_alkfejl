package com.example.kosarlabda;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddMatchActivity extends AppCompatActivity {

    private EditText teamAEditText, scoreAEditText, teamBEditText, scoreBEditText, locationEditText, dateTimeEditText;
    private Button addMatchButton;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final String PREF_NAME = "match_draft";

    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addmatch);

        // Toolbar setup with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();



        // Bind UI elements
        teamAEditText = findViewById(R.id.teamAEditText);
        scoreAEditText = findViewById(R.id.scoreAEditText);
        teamBEditText = findViewById(R.id.teamBEditText);
        scoreBEditText = findViewById(R.id.scoreBEditText);
        locationEditText = findViewById(R.id.locationEditText);
        dateTimeEditText = findViewById(R.id.dateTimeEditText);
        dateTimeEditText.setFocusable(false);
        dateTimeEditText.setClickable(true);
        dateTimeEditText.setOnClickListener(v -> showDateTimePicker());
        addMatchButton = findViewById(R.id.addMatchButton);
        progressBar = findViewById(R.id.progressBar);

        addMatchButton.setOnClickListener(v -> addMatch());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void showDateTimePicker() {
        final Calendar current = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth, hourOfDay, minute);
                Date selectedDate = chosen.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                dateTimeEditText.setText(sdf.format(selectedDate));

                // 🔍 Dátum összehasonlítása és mezők engedélyezése/letiltása
                if (selectedDate.after(new Date())) {
                    scoreAEditText.setEnabled(false);
                    scoreBEditText.setEnabled(false);
                    scoreAEditText.setText("");  // opcionálisan üríti a mezőt
                    scoreBEditText.setText("");
                    Toast.makeText(this, "Jövőbeli időpont — eredmény nem adható meg.", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (!alarmManager.canScheduleExactAlarms()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Pontos riasztás szükséges")
                                    .setMessage("Ahhoz, hogy az alkalmazás pontos időben értesítést tudjon küldeni (pl. mérkőzés előtt), engedélyezd a pontos riasztást az alkalmazás beállításaiban.")
                                    .setPositiveButton("Beállítások megnyitása", (dialog, which) -> {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    })
                                    .setNegativeButton("Mégse", null)
                                    .show();
                        }
                    }

                } else {
                    scoreAEditText.setEnabled(true);
                    scoreBEditText.setEnabled(true);
                }

            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        teamAEditText.setText(prefs.getString("teamA", ""));
        scoreAEditText.setText(prefs.getString("scoreA", ""));
        teamBEditText.setText(prefs.getString("teamB", ""));
        scoreBEditText.setText(prefs.getString("scoreB", ""));
        locationEditText.setText(prefs.getString("location", ""));
        dateTimeEditText.setText(prefs.getString("dateTime", ""));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putString("teamA", teamAEditText.getText().toString());
        editor.putString("scoreA", scoreAEditText.getText().toString());
        editor.putString("teamB", teamBEditText.getText().toString());
        editor.putString("scoreB", scoreBEditText.getText().toString());
        editor.putString("location", locationEditText.getText().toString());
        editor.putString("dateTime", dateTimeEditText.getText().toString());
        editor.apply();
    }

    private void addMatch() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Kérlek jelentkezz be a hozzáféréshez!", Toast.LENGTH_SHORT).show();
            return;
        }
        String ownerUid = currentUser.getUid();

        String teamA = teamAEditText.getText().toString().trim();
        String scoreAStr = scoreAEditText.getText().toString().trim();
        String teamB = teamBEditText.getText().toString().trim();
        String scoreBStr = scoreBEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String dateTimeStr = dateTimeEditText.getText().toString().trim();

        if (teamA.isEmpty() || teamB.isEmpty() || location.isEmpty() || dateTimeStr.isEmpty()) {
            Toast.makeText(this, "Kérlek, tölts ki minden mezőt!", Toast.LENGTH_SHORT).show();
            return;
        }

        Date dateTime;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateTime = sdf.parse(dateTimeStr);
        } catch (ParseException e) {
            Toast.makeText(this, "Dátum formátum: yyyy-MM-dd HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isFutureMatch = dateTime.after(new Date());

        int scoreA = 0;
        int scoreB = 0;

        if (!isFutureMatch) {
            if (scoreAStr.isEmpty() || scoreBStr.isEmpty()) {
                Toast.makeText(this, "Kérlek, add meg az eredményt!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                scoreA = Integer.parseInt(scoreAStr);
                scoreB = Integer.parseInt(scoreBStr);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Pontszám csak szám lehet!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        addMatchButton.setEnabled(false);

        DocumentReference docRef = db.collection("matches").document();
        String id = docRef.getId();

        Match match = new Match(id, teamA, scoreA, teamB, scoreB, location, dateTime, ownerUid);



        docRef.set(match)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    addMatchButton.setEnabled(true);
                    Toast.makeText(AddMatchActivity.this, "Mérkőzés sikeresen hozzáadva", Toast.LENGTH_SHORT).show();
                    teamAEditText.setText("");
                    scoreAEditText.setText("");
                    teamBEditText.setText("");
                    scoreBEditText.setText("");
                    locationEditText.setText("");
                    dateTimeEditText.setText("");
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    addMatchButton.setEnabled(true);
                    Toast.makeText(AddMatchActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });





        if (isFutureMatch) {
            long matchTime = match.getDateTime().getTime();

            // Mérkőzéskor
            scheduleNotification(
                    matchTime,
                    "KosárlabdApp",
                    "Kezdődik a mérkőzés: " + teamA + " vs " + teamB,
                    1001  // Egyedi requestCode
            );


            long currentTime = System.currentTimeMillis();

            // Csak akkor állítsuk be, ha még több mint 30 perc van hátra
            if (matchTime - currentTime > 30 * 60 * 1000) {
                // Fél órával előtte
                scheduleNotification(
                        matchTime - 30 * 60 * 1000,
                        "KosárlabdApp",
                        "A " + match.getTeamA() + " vs " + match.getTeamB() + " mérkőzés fél óra múlva kezdődik.",
                        1002
                );
            }

        }


    }


    private void scheduleNotification(long triggerAtMillis, String title, String message, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, MatchNotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,  // Fontos: egyedi requestCode minden külön értesítéshez!
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) {
            Toast.makeText(this, "Hiba: AlarmManager nem elérhető.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // API 31+ (S) esetén: pontos riasztások engedélyezésének ellenőrzése
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent alarmIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(alarmIntent);
                    Toast.makeText(this, "Engedélyezd a pontos riasztásokat!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );

            Log.d("AddMatchActivity", "Riasztás beállítva: " + title + " - " + new Date(triggerAtMillis));

        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Nincs engedély pontos riasztásokhoz.", Toast.LENGTH_SHORT).show();
        }
    }



}
