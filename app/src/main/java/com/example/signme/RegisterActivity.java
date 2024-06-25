package com.example.signme;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;


public class RegisterActivity extends AppCompatActivity {

    private ConnectionClass connectionClass;
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        connectionClass = new ConnectionClass();
        // Initialize views
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);

        TextView loginTextView = findViewById(R.id.loginTextView);
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Set styled text for logo
        TextView logoText = findViewById(R.id.logoText);
        SpannableString spannableString = new SpannableString("SignMe");
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#ADD8E6")), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Light blue for "Sign"
        spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 4, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // White for "Me"
        logoText.setText(spannableString);

        // Set click listener for register button
        registerButton.setOnClickListener(v -> registerUser());

        // Add text change listener to confirm password field
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswords();
            }
        });

        // Add text change listener to email field for real-time validation
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validatePassword(password)) {
            Toast.makeText(this, "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, and one special character", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Encrypt the password using bcrypt before saving
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Insert user data into the database
        saveUserData(firstName, lastName, email, hashedPassword);
    }

    private void saveUserData(String firstName, String lastName, String email, String password) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "INSERT INTO driver (FIRST_NAME, LAST_NAME, EMAIL, PASSWORD) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setString(4, password);

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show());
                    // Navigate to HomeActivity
                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    startActivity(intent);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show());
                }

                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void validateEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email address");
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "SELECT EMAIL FROM driver WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    runOnUiThread(() -> emailEditText.setError("Email address already exists"));
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private boolean validatePassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).{8,}$");
        return pattern.matcher(password).matches();
    }

    private void validatePasswords() {
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
        }
    }
}
