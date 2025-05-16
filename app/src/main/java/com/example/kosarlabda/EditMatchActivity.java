
package com.example.kosarlabda;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditMatchActivity extends AppCompatActivity {

    private EditText teamAEditText, scoreAEditText, teamBEditText, scoreBEditText, locationEditText, dateTimeEditText;
    private Button updateMatchButton;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    private String matchId;
    private Match currentMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editmatch);

        db = FirebaseFirestore.getInstance();

        // Get match ID
        matchId = getIntent().getStringExtra("matchId");
        if (matchId == null) {
            Toast.makeText(this, "Hiányzó mérkőzés azonosító", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Init UI
        teamAEditText = findViewById(R.id.teamAEditText);
        scoreAEditText = findViewById(R.id.scoreAEditText);
        teamBEditText = findViewById(R.id.teamBEditText);
        scoreBEditText = findViewById(R.id.scoreBEditText);
        locationEditText = findViewById(R.id.locationEditText);
        dateTimeEditText = findViewById(R.id.dateTimeEditText);
        dateTimeEditText.setFocusable(false);
        dateTimeEditText.setOnClickListener(v -> showDateTimePicker());

        updateMatchButton = findViewById(R.id.updateMatchButton);
        progressBar = findViewById(R.id.progressBar);

        // Load existing match data
        loadMatch();

        updateMatchButton.setOnClickListener(v -> updateMatch());
    }

    private void loadMatch() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("matches").document(matchId).get()
                .addOnSuccessListener(document -> {
                    progressBar.setVisibility(View.GONE);
                    if (document.exists()) {
                        currentMatch = document.toObject(Match.class);
                        if (currentMatch != null) {
                            teamAEditText.setText(currentMatch.getTeamA());
                            scoreAEditText.setText(String.valueOf(currentMatch.getScoreA()));
                            teamBEditText.setText(currentMatch.getTeamB());
                            scoreBEditText.setText(String.valueOf(currentMatch.getScoreB()));
                            locationEditText.setText(currentMatch.getLocation());
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            dateTimeEditText.setText(sdf.format(currentMatch.getDateTime()));
                        }
                    } else {
                        Toast.makeText(this, "Mérkőzés nem található", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showDateTimePicker() {
        final Calendar current = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, dayOfMonth, hourOfDay, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                dateTimeEditText.setText(sdf.format(chosen.getTime()));
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateMatch() {
        String teamA = teamAEditText.getText().toString().trim();
        String scoreAStr = scoreAEditText.getText().toString().trim();
        String teamB = teamBEditText.getText().toString().trim();
        String scoreBStr = scoreBEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String dateTimeStr = dateTimeEditText.getText().toString().trim();

        if (teamA.isEmpty() || scoreAStr.isEmpty() || teamB.isEmpty() || scoreBStr.isEmpty() || location.isEmpty() || dateTimeStr.isEmpty()) {
            Toast.makeText(this, "Kérlek, tölts ki minden mezőt!", Toast.LENGTH_SHORT).show();
            return;
        }

        int scoreA, scoreB;
        try {
            scoreA = Integer.parseInt(scoreAStr);
            scoreB = Integer.parseInt(scoreBStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Pontszám csak szám lehet!", Toast.LENGTH_SHORT).show();
            return;
        }

        Date dateTime;
        try {
            dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateTimeStr);
        } catch (ParseException e) {
            Toast.makeText(this, "Helytelen dátum formátum", Toast.LENGTH_SHORT).show();
            return;
        }

        // Frissítés
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("teamA", teamA);
        updatedData.put("scoreA", scoreA);
        updatedData.put("teamB", teamB);
        updatedData.put("scoreB", scoreB);
        updatedData.put("location", location);
        updatedData.put("dateTime", dateTime);

        progressBar.setVisibility(View.VISIBLE);
        db.collection("matches").document(matchId).update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Mérkőzés frissítve", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

