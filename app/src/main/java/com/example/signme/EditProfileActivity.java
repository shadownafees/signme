package com.example.signme;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText dobEditText;
    private Button saveChangesButton;

    private ConnectionClass connectionClass;
    private Connection con;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        connectionClass = new ConnectionClass();

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        saveChangesButton = findViewById(R.id.saveChangesButton);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = intent.getStringExtra("email");
            fetchUserData();
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        dobEditText.setOnClickListener(v -> showDatePickerDialog());

        saveChangesButton.setOnClickListener(v -> saveChanges());
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year1;
                    dobEditText.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void fetchUserData() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "SELECT FIRST_NAME, LAST_NAME, DATE_OF_BIRTH FROM driver WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String firstName = rs.getString("FIRST_NAME");
                    String lastName = rs.getString("LAST_NAME");
                    String dob = rs.getString("DATE_OF_BIRTH");

                    runOnUiThread(() -> {
                        firstNameEditText.setText(firstName);
                        lastNameEditText.setText(lastName);
                        dobEditText.setText(dob);
                    });
                }

                rs.close();
                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveChanges() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "UPDATE driver SET FIRST_NAME = ?, LAST_NAME = ?, DATE_OF_BIRTH = ? WHERE EMAIL = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, dob);
                stmt.setString(4, email);

                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show());
                }

                stmt.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "SQL Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
