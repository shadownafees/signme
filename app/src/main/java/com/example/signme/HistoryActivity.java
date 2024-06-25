package com.example.signme;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private ConnectionClass connectionClass;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize views
        TextView headingText = findViewById(R.id.headingText);
        historyContainer = findViewById(R.id.historyContainer);
        connectionClass = new ConnectionClass();
        executorService = Executors.newSingleThreadExecutor();

        // Set heading text
        headingText.setText("History");

        // Fetch session data
        fetchSessionData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshPage() {
        // Clear existing data
        historyContainer.removeAllViews();
        // Fetch session data again
        fetchSessionData();
    }

    private void fetchSessionData() {
        final String email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            showToast("No email found");
            return;
        }
        final String trimmedEmail = email.trim();  // Trim any leading or trailing whitespace

        executorService.execute(() -> {
            Connection con = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> showToast("Failed to connect to database"));
                    return;
                }

                // Database connection successful message
                runOnUiThread(() -> showToast("Database connection successful"));

                // Get today's and yesterday's dates
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                String today = dateFormat.format(calendar.getTime());

                calendar.add(Calendar.DAY_OF_YEAR, -1);
                String yesterday = dateFormat.format(calendar.getTime());

                // Query to fetch session data
                String query = "SELECT START_LOCATION, DESTINATION, SESSION_START_TIME, SESSION_END_TIME " +
                        "FROM session " +
                        "WHERE EMAIL = ? " +
                        "ORDER BY SESSION_START_TIME DESC";

                stmt = con.prepareStatement(query);
                stmt.setString(1, trimmedEmail);

                rs = stmt.executeQuery();

                boolean foundSessions = false;
                while (rs.next()) {
                    foundSessions = true;
                    String startLocation = rs.getString("START_LOCATION");
                    String destination = rs.getString("DESTINATION");
                    String sessionStartTime = rs.getString("SESSION_START_TIME");
                    String sessionEndTime = rs.getString("SESSION_END_TIME");

                    // Determine session date
                    String sessionDate = sessionStartTime.substring(0, 10); // Extract "dd-MM-yyyy" from timestamp

                    // Add session to the appropriate section
                    runOnUiThread(() -> addSession(sessionDate, startLocation, destination, sessionStartTime, sessionEndTime));
                }

                if (!foundSessions) {
                    runOnUiThread(() -> showToast("No sessions found for this email"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("SQL Exception: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Exception: " + e.getMessage()));
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                executorService.shutdown();
            }
        });
    }

    private void addSession(String sessionDate, String startLocation, String destination, String startTime, String endTime) {
        // Create or find the appropriate section view
        LinearLayout sectionLayout = findOrCreateSection(sessionDate);

        // Create session view
        LinearLayout sessionLayout = new LinearLayout(this);
        sessionLayout.setOrientation(LinearLayout.VERTICAL);
        sessionLayout.setBackground(getResources().getDrawable(R.drawable.rounded_rectangle));
        sessionLayout.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16);
        sessionLayout.setLayoutParams(layoutParams);

        // Starting Point - Destination (Subheading)
        TextView locationText = new TextView(this);
        locationText.setText(startLocation + " - " + destination);
        locationText.setTextColor(getResources().getColor(android.R.color.white));
        locationText.setTextSize(20);
        locationText.setTypeface(null, Typeface.BOLD);
        sessionLayout.addView(locationText);

        // Drive duration
        TextView durationText = new TextView(this);
        durationText.setText(calculateDuration(startTime, endTime));
        durationText.setTextColor(getResources().getColor(android.R.color.white));
        durationText.setTypeface(null, Typeface.ITALIC);
        sessionLayout.addView(durationText);

        // Session Start Time - End Time
        TextView timeText = new TextView(this);
        timeText.setText(formatTime(startTime) + " - " + formatTime(endTime));
        timeText.setTextColor(getResources().getColor(android.R.color.white));
        sessionLayout.addView(timeText);

        // Add session view to the section
        sectionLayout.addView(sessionLayout);
    }

    private LinearLayout findOrCreateSection(String sessionDate) {
        // Check if the section already exists
        LinearLayout sectionLayout = findSectionByDate(sessionDate);

        if (sectionLayout == null) {
            // Create a new section for the date
            sectionLayout = new LinearLayout(this);
            sectionLayout.setOrientation(LinearLayout.VERTICAL);
            sectionLayout.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 32, 0, 0); // Add margin between sections
            sectionLayout.setLayoutParams(layoutParams);

            // Date heading
            TextView dateHeading = new TextView(this);
            dateHeading.setText(formatSectionDate(sessionDate));
            dateHeading.setTextColor(getResources().getColor(android.R.color.white));
            dateHeading.setTextSize(18);
            dateHeading.setTypeface(null, Typeface.BOLD);
            sectionLayout.addView(dateHeading);

            // Add the section to the history container
            historyContainer.addView(sectionLayout);
        }

        return sectionLayout;
    }

    private LinearLayout findSectionByDate(String sessionDate) {
        // Search for existing section with the given date
        for (int i = 0; i < historyContainer.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) historyContainer.getChildAt(i);
            if (child.getChildCount() > 0) {
                TextView dateHeading = (TextView) child.getChildAt(0);
                if (dateHeading.getText().toString().equals(formatSectionDate(sessionDate))) {
                    return child;
                }
            }
        }
        return null;
    }

    private String formatSectionDate(String sessionDate) {
        // Format the session date for section heading
        if (sessionDate.equals(getTodayDate())) {
            return "Today";
        } else if (sessionDate.equals(getYesterdayDate())) {
            return "Yesterday";
        } else {
            return sessionDate;
        }
    }

    private String getTodayDate() {
        // Get today's date in "dd-MM-yyyy" format
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private String getYesterdayDate() {
        // Get yesterday's date in "dd-MM-yyyy" format
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private String calculateDuration(String startTime, String endTime) {
        // Calculate duration between start time and end time
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        try {
            Date start = format.parse(startTime);
            Date end = format.parse(endTime);
            long duration = end.getTime() - start.getTime();
            long diffInMinutes = duration / (1000 * 60);
            long hours = diffInMinutes / 60;
            long minutes = diffInMinutes % 60;
            String durationString = "";
            if (hours > 0) {
                durationString += hours + " hour ";
            }
            durationString += minutes + " minutes drive";
            return durationString;
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String formatTime(String time) {
        // Format time from "dd-MM-yyyy HH:mm:ss" to "HH:mm"
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(time);
            return outputFormat.format(date);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void showToast(String message) {
        // Display toast message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executorService when activity is destroyed
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
