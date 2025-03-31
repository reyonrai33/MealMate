
package edu.suresh.mealmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import edu.suresh.mealmate.home.DashboardActivity;
import edu.suresh.mealmate.home.MainActivity;
import edu.suresh.mealmate.home.ProfileActivity;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // 1-second delay before navigating to the next screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen); // Set the layout for splash screen

        // Handler to delay the transition to the next screen by SPLASH_DELAY milliseconds
        new Handler().postDelayed(() -> {

            // Retrieve user data from SharedPreferences to check if user is logged in
            SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = sharedPref.getString("USER_ID", null); // Get the stored user ID

            // Check if user is logged in by checking if the user ID exists
            if (userId != null) {
                // User is logged in, now check if all the necessary details are available
                String name = sharedPref.getString("USER_NAME", null);
                String mobile = sharedPref.getString("USER_MOBILE", null);
                String dob = sharedPref.getString("USER_DOB", null);
                String gender = sharedPref.getString("USER_GENDER", null);
                String photo = sharedPref.getString("USER_PHOTO", null);

                Intent intent;

                // If all user details are available, navigate to the Dashboard
                if (name != null && mobile != null && dob != null && gender != null && photo != null) {
                    intent = new Intent(SplashScreen.this, DashboardActivity.class);
                } else {
                    // If some details are missing, navigate to ProfileActivity to complete the profile
                    intent = new Intent(SplashScreen.this, ProfileActivity.class);
                }

                // Clear previous activities from the back stack and start the new activity
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // If no user is logged in, navigate to MainActivity (Login/Signup screen)
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            // Close the SplashScreen activity so that the user cannot go back to it
            finish();
        }, SPLASH_DELAY); // Delay for 1 second before executing the code
    }
}
