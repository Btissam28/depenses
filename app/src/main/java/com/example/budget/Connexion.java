package com.example.budget;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Connexion extends AppCompatActivity {
    private EditText etLogin, etPassword;
    private Button bLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexion);

        etLogin = findViewById(R.id.etMail);
        etPassword = findViewById(R.id.etPassword);
        bLogin = findViewById(R.id.bLogin);
        tvRegister = findViewById(R.id.tvRegister);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        bLogin.setOnClickListener(view -> loginUser());

        tvRegister.setOnClickListener(view ->
                startActivity(new Intent(Connexion.this, Register.class)));
    }

    private void loginUser() {
        String email = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etLogin.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Vérifier si c'est la première connexion
                            checkAndInitializeUserData(user.getUid());
                        }
                    } else {
                        Toast.makeText(Connexion.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndInitializeUserData(String userId) {
        DatabaseReference userDataRef = usersRef.child(userId);

        userDataRef.child("solde").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Première connexion - initialiser les données
                    initializeUserData(userId);
                } else {
                    // Utilisateur existant - aller à MainActivity
                    proceedToMainActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Connexion.this,
                        "Error checking user data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeUserData(String userId) {
        DatabaseReference userRef = usersRef.child(userId);

        // Initialiser toutes les données à zéro/vide
        userRef.child("solde").setValue(0.0);
        userRef.child("expenses").setValue(null); // Effacer les dépenses existantes
        userRef.child("incomes").setValue(null);  // Effacer les revenus existants

        // Initialiser les catégories par défaut
        DatabaseReference categoriesRef = userRef.child("categories");
        String[] defaultCategories = {
                "Alimentation", "Transport", "Logement", "Santé",
                "Éducation", "Loisirs", "Shopping", "Autres"
        };

        for (String category : defaultCategories) {
            categoriesRef.push().setValue(category);
        }

        Toast.makeText(this, "Initialisation des données terminée", Toast.LENGTH_SHORT).show();
        proceedToMainActivity();
    }

    private void proceedToMainActivity() {
        startActivity(new Intent(Connexion.this, MainActivity.class));
        finish();
    }
}