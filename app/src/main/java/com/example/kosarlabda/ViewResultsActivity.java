package com.example.kosarlabda;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ViewResultsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private MatchAdapter adapter;
    private List<Match> matchList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewresults);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // UI elements
        recyclerView = findViewById(R.id.recyclerViewMatches);
        emptyView = findViewById(R.id.emptyView);

        // RecyclerView setup
        matchList = new ArrayList<>();
        adapter = new MatchAdapter(matchList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMatches();
    }

    private void loadMatches() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Kérlek jelentkezz be!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String uid = currentUser.getUid();

        // Query matches where ownerUid == uid
        db.collection("matches")
                .whereEqualTo("ownerUid", uid)
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this, (QuerySnapshot snapshots) -> {
                    matchList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Match match = doc.toObject(Match.class);
                        match.setId(doc.getId());
                        matchList.add(match);
                    }
                    adapter.notifyDataSetChanged();
                    emptyView.setVisibility(matchList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Hiba a lekérdezés során: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Adapter inner class
    private static class MatchAdapter extends RecyclerView.Adapter<MatchViewHolder> {
        private final List<Match> matches;

        MatchAdapter(List<Match> matches) {
            this.matches = matches;
        }

        @NonNull
        @Override
        public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false);
            return new MatchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
            holder.bind(matches.get(position));
        }

        @Override
        public int getItemCount() {
            return matches.size();
        }
    }

    // ViewHolder class
    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        private final TextView teamsTextView;
        private final TextView scoreTextView;
        private final TextView metaTextView;
        private final ImageView editIcon;
        private final ImageView deleteIcon;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            teamsTextView = itemView.findViewById(R.id.teamsTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
            metaTextView = itemView.findViewById(R.id.metaTextView);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }

        public void bind(Match match) {
            teamsTextView.setText(match.getTeamA() + " vs " + match.getTeamB());
            scoreTextView.setText(match.getScoreA() + " - " + match.getScoreB());
            metaTextView.setText(match.getLocation() + ", " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(match.getDateTime()));

            editIcon.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, EditMatchActivity.class);
                intent.putExtra("matchId", match.getId());
                context.startActivity(intent);
            });

            deleteIcon.setOnClickListener(v -> {
                Context context = v.getContext();
                new AlertDialog.Builder(context)
                        .setTitle("Törlés megerősítése")
                        .setMessage("Biztosan törölni szeretnéd ezt a mérkőzést?")
                        .setPositiveButton("Igen", (dialog, which) -> {
                            FirebaseFirestore.getInstance().collection("matches")
                                    .document(match.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Mérkőzés törölve", Toast.LENGTH_SHORT).show();
                                        // Frissítés a törlés után
                                        if (context instanceof ViewResultsActivity) {
                                            ((ViewResultsActivity) context).loadMatches();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Mégse", null)
                        .show();
            });
        }
    }


}
