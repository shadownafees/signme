package com.example.signme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveActivity extends AppCompatActivity {

    ConnectionClass connectionClass;
    Connection con;

    private AutoCompleteTextView startingPointAutoComplete;
    private AutoCompleteTextView destinationAutoComplete;
    private Button startButton;
    private String userEmail; // Declare a variable to hold the email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        connectionClass = new ConnectionClass(); // Initialize your connection class properly

        // Initialize views
        startingPointAutoComplete = findViewById(R.id.startingPoint);
        destinationAutoComplete = findViewById(R.id.destination);
        startButton = findViewById(R.id.startButton);

        // Setup autocomplete for Starting Point and Destination
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, getSriLankanCities());
        startingPointAutoComplete.setAdapter(adapter);
        destinationAutoComplete.setAdapter(adapter);

        // Receive the email from HomeActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            userEmail = intent.getStringExtra("email");
        } else {
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            // Handle the case where email is not passed correctly
            return;
        }

        // Set click listener for Start button
        startButton.setOnClickListener(v -> startSession());
    }

    // Method to start a new session
    private void startSession() {
        String startingPoint = startingPointAutoComplete.getText().toString().trim();
        String destination = destinationAutoComplete.getText().toString().trim();
        String vehicleType = "Car"; // Example: Set vehicle type based on selection or default

        if (startingPoint.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "Please enter Starting Point and Destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate SESSION_ID in format DDMMYYYY-HHmm (e.g., 01122023-1223)
        String sessionId = generateSessionId();

        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String sessionStartTime = sdf.format(new Date());

        // Insert session data into the database
        saveSessionData(userEmail, sessionId, startingPoint, destination, vehicleType, sessionStartTime);
    }

    private void saveSessionData(String email, String sessionId, String startingPoint, String destination, String vehicleType, String sessionStartTime) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error in connection with MySQL server", Toast.LENGTH_SHORT).show());
                    return;
                }

                String query = "INSERT INTO session (EMAIL, SESSION_ID, START_LOCATION, DESTINATION, VEHICLE_TYPE, SESSION_START_TIME) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, email);
                stmt.setString(2, sessionId);
                stmt.setString(3, startingPoint);
                stmt.setString(4, destination);
                stmt.setString(5, vehicleType);
                stmt.setString(6, sessionStartTime);

                int rowsInserted = stmt.executeUpdate();
                runOnUiThread(() -> {
                    if (rowsInserted > 0) {
                        Toast.makeText(this, "Session started and saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to start session", Toast.LENGTH_SHORT).show();
                    }
                });

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

    // Method to retrieve Sri Lankan cities for autocomplete suggestions
    private String[] getSriLankanCities() {
        return new String[]{
                "Mihinthale",
                "Dharmalokagama Junction",
                "Matale Junction",
                "Jaffna Junction",
                "Old Town Market",
                "Market",
                "Bank Town",
                "Anuradhapura New Town"
        };
    }

    // Method to generate SESSION_ID in format DDMMYYYY-HHmm
    private String generateSessionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return sdf.format(new Date());
    }
}
