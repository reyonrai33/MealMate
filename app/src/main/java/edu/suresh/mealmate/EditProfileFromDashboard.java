package edu.suresh.mealmate;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.suresh.mealmate.home.DashboardActivity;
import edu.suresh.mealmate.home.ProfileActivity;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileFromDashboard extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final String IMGUR_CLIENT_ID = "e115074dd9cfe62"; // Replace with your Client ID
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";

    AutoCompleteTextView etGender;
    private ImageView profileImage;
    private EditText etFullName, etDob, etMobile, etAge;
    private Button btnUploadPhoto, btnSaveProfile;
    private Uri selectedImageUri = null;
    private Uri cameraImageUri;
    private String uploadedImageUrl = "";  // Default empty if no image selected
    private String fullName, dob, mobile, age;
    private CustomProgressDialog progressDialog;

    String photoPicUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile_from_dashboard);

        profileImage = findViewById(R.id.profileImage);
        etFullName = findViewById(R.id.etFullName);
        etDob = findViewById(R.id.etDob);
        etMobile = findViewById(R.id.etMobile);
        etAge = findViewById(R.id.etAge);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        etGender = findViewById(R.id.etGender);

        progressDialog = new CustomProgressDialog(this); // Initialize Progress Dialog

        setupGenderDropdown();
        loadLocalProfile();

        etDob.setOnClickListener(v -> showDatePicker());
        btnUploadPhoto.setOnClickListener(v -> showImagePickerDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showImagePickerDialog() {
        Intent pickGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickGalleryIntent.setType("image/*");

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        Intent chooser = Intent.createChooser(pickGalleryIntent, "Select or Capture Image");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        startActivityForResult(chooser, REQUEST_GALLERY);
    }

    // Handle Image Selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                selectedImageUri = data.getData();
            } else if (requestCode == REQUEST_CAMERA) {
                selectedImageUri = cameraImageUri;
            }

            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    profileImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    showSnackbar("Error loading image.");
                }
            }
        }
    }


    private void saveProfile() {
        fullName = etFullName.getText().toString();
        dob = etDob.getText().toString();
        mobile = etMobile.getText().toString();
        age = etAge.getText().toString();
        String gender = etGender.getText().toString();

        if (fullName.isEmpty() || dob.isEmpty() || mobile.isEmpty()) {
            showSnackbar("Please fill in all fields.");
            return;
        }

        if (gender.isEmpty()) {
            gender = "Other"; // Default value
        }

        if (!isValidGender(gender)) {
            showSnackbar("Please select a valid gender from the dropdown.");
            return;
        }


        if (selectedImageUri != null) {
            uploadImageToImgur(selectedImageUri);
        }

        else if (photoPicUrl != null && !photoPicUrl.isEmpty()) {
            saveUserData(photoPicUrl, gender);
        }

        else {
            saveUserData("", gender);
        }
    }


    private void uploadImageToImgur(Uri imageUri) {
        progressDialog.show();
        File file = getFileFromUri(imageUri);

        if (file == null || !file.exists()) {
            showSnackbar("Error: Unable to process image.");
            return;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(MediaType.parse("image/*"), file))
                .build();

        Request request = new Request.Builder()
                .url(IMGUR_UPLOAD_URL)
                .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                .post(requestBody)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    String imgUrl = extractImageUrl(responseBody);
                    runOnUiThread(() -> saveUserData(imgUrl, etGender.getText().toString().trim())); // Pass trimmed gender
                } else {
                    runOnUiThread(() -> showSnackbar("Upload failed!"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> showSnackbar("Network error. Try again."));
            }
            progressDialog.show();
        }).start();
    }


    private void saveUserData(String photoUrl, String gender) {
        progressDialog.show();
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", null);

        if (userId == null) {
            showSnackbar("Error: User ID not found.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("mobile", mobile);
        user.put("dob", dob);
        user.put("gender", gender.trim()); // Trim the gender value
        user.put("photoUrl", photoUrl);

        db.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showSnackbar("Profile saved to Firebase! ✅");

                    savePref(fullName,mobile,dob,gender.trim(),photoUrl);

                    // Navigate to DashboardActivity and remove all previous activities
                    //Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //startActivity(intent);
                    Intent resultIntent = new Intent();

                    setResult(RESULT_OK, resultIntent);
                    finish(); // Close the current activity
                })
                .addOnFailureListener(e -> showSnackbar("Error saving profile!"));
        progressDialog.dismiss();
    }

    private void savePref(String name, String mobile, String dob, String gender, String photoUrl){
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("USER_NAME", name);
        editor.putString("USER_MOBILE", mobile);
        editor.putString("USER_DOB", dob);
        editor.putString("USER_GENDER", gender);
        editor.putString("USER_PHOTO", photoUrl);

        editor.apply();
    }

    private File getFileFromUri(Uri uri) {
        try {
            // ✅ Create a temporary file in cache directory
            File tempFile = new File(getCacheDir(), "upload_image.jpg");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(tempFile);

            // ✅ Copy file contents
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d("FILE_PATH", "Temp file created: " + tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 🎯 Extract Image URL from Imgur Response
    private String extractImageUrl(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            return jsonObject.getJSONObject("data").getString("link");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }




    void setupGenderDropdown() {
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);

        if (etGender != null) {
            etGender.setAdapter(adapter);
            etGender.setOnClickListener(v -> etGender.showDropDown());
        } else {
            Log.e("EditProfileActivity", "Gender dropdown not found!");
        }
    }

    void loadLocalProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", null);
        String name = sharedPreferences.getString("USER_NAME", null);
        String mobile = sharedPreferences.getString("USER_MOBILE", null);
        String dob = sharedPreferences.getString("USER_DOB", null);
        String gender = sharedPreferences.getString("USER_GENDER", null);
        photoPicUrl = sharedPreferences.getString("USER_PHOTO", null);

        if (photoPicUrl != null && !photoPicUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoPicUrl)
                    .placeholder(R.drawable.ic_men) // Fallback image
                    .error(R.drawable.profile_border) // Error case
                    .into(profileImage);
        }

        etFullName.setText(name);
        etMobile.setText(mobile);

        if (gender != null) {
            etGender.setText(gender, false); // Set gender without triggering dropdown
        }

        if (dob != null) {
            etDob.setText(dob); // Set existing DOB
            etAge.setText(String.valueOf(calculateAge(dob))); // Calculate & set age
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        // Retrieve DOB from EditText (SharedPreferences already pre-filled it)
        String existingDob = etDob.getText().toString();

        int year, month, day;

        if (!existingDob.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                Date date = sdf.parse(existingDob);
                calendar.setTime(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            // Default to 18 years old if no DOB is saved
            calendar.set(Calendar.YEAR, currentYear - 18);
        }

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    etDob.setText(selectedDate);

                    // Calculate and update age
                    int calculatedAge = currentYear - selectedYear;
                    etAge.setText(String.valueOf(calculatedAge));
                }, year, month, day);

        datePickerDialog.getDatePicker().setCalendarViewShown(false);
        datePickerDialog.getDatePicker().setSpinnersShown(true);
        datePickerDialog.show();
    }

    private int calculateAge(String dob) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date birthDate = sdf.parse(dob);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--; // Adjust if birthday hasn't occurred yet this year
            }
            return age;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.primary_variant))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }

    private boolean isValidGender(String gender) {
        String[] validGenders = new String[]{"Male", "Female", "Other"};
        for (String validGender : validGenders) {
            if (validGender.equalsIgnoreCase(gender)) {
                return true;
            }
        }
        return false;
    }

}
