package com.example.kosarlabda;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import com.airbnb.lottie.LottieAnimationView;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, passwordEditText2;
    private Button registerButton, backButton;
    private LottieAnimationView loadingAnimation;
    private TextView successMessageTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect layout elements
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        passwordEditText2 = findViewById(R.id.passwordEditText2);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        successMessageTextView = findViewById(R.id.successMessageTextView);
        // New textView

        backButton.setOnClickListener(v -> finish());

        // Register button event
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String password2 = passwordEditText2.getText().toString().trim();


            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Minden mezőt ki kell tölteni!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(password2)) {
                Toast.makeText(RegisterActivity.this, "A jelszavaknak meg kell egyezni!", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        loadingAnimation.cancelAnimation();
                        loadingAnimation.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                           return;
                        }
                        String uid = user.getUid();

                        // User data object
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("createdAt", FieldValue.serverTimestamp());


                        // Save to Firestore
                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    successMessageTextView.setVisibility(View.VISIBLE); // Make it visible
                                    successMessageTextView.setText("Sikeres regisztráció! Átirányítás...");



                                    new Handler().postDelayed(() -> {
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }, 2000);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterActivity.this, "Hiba Firestore mentésnél: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
        });
    }
}
