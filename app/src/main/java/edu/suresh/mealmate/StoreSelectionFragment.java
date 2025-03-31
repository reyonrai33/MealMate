package edu.suresh.mealmate.fragments;

import android.Manifest;  // Import for the Manifest class to access permissions
import android.content.pm.PackageManager;  // Import for checking permissions
import android.location.Location;  // Import for Location class to get the current device's location
import android.os.Bundle;  // Import for Bundle class to pass data between fragments
import android.text.Editable;  // Import for Editable class to manage text input
import android.text.TextWatcher;  // Import for TextWatcher interface to listen for text changes
import android.util.Log;  // Import for logging debug and error messages
import android.view.LayoutInflater;  // Import for LayoutInflater to inflate layout XML files
import android.view.View;  // Import for View class to handle UI elements
import android.view.ViewGroup;  // Import for ViewGroup class to manage UI containers
import android.widget.RadioGroup;  // Import for RadioGroup to handle radio buttons
import android.widget.Toast;  // Import for Toast to display short messages
import android.widget.ViewSwitcher;  // Import for ViewSwitcher to switch between views

import androidx.annotation.NonNull;  // Import for annotation to indicate non-null variables
import androidx.annotation.Nullable;  // Import for annotation to indicate nullable variables
import androidx.core.app.ActivityCompat;  // Import for ActivityCompat to check and request permissions
import androidx.fragment.app.Fragment;  // Import for Fragment class to define a part of an activity
import androidx.recyclerview.widget.LinearLayoutManager;  // Import for LinearLayoutManager to manage RecyclerView layout
import androidx.recyclerview.widget.RecyclerView;  // Import for RecyclerView to display a list of items

import com.google.android.gms.location.FusedLocationProviderClient;  // Import for the fused location provider to get device location
import com.google.android.gms.location.LocationServices;  // Import for accessing the location services API
import com.google.android.material.snackbar.Snackbar;  // Import for Snackbar to show brief messages at the bottom of the screen
import com.google.android.material.textfield.TextInputEditText;  // Import for TextInputEditText to handle text input in a text field
import com.google.firebase.firestore.FirebaseFirestore;  // Import for Firebase Firestore to interact with Firestore database
import com.google.firebase.firestore.QueryDocumentSnapshot;  // Import for querying Firestore documents

import java.util.ArrayList;  // Import for ArrayList to store lists of objects
import java.util.List;  // Import for List interface to store multiple objects
import java.util.Locale;  // Import for Locale class to format location and distance in different locales

import edu.suresh.mealmate.R;  // Import for R class to reference resources like layouts and IDs
import edu.suresh.mealmate.adapters.StoreSelectionAdapter;  // Import for StoreSelectionAdapter class to handle store data in RecyclerView
import edu.suresh.mealmate.model.SavedLocation;  // Import for SavedLocation model to store store details

public class StoreSelectionFragment extends Fragment {

    // Declare UI components for the fragment
    private RecyclerView recyclerViewStores;  // RecyclerView for displaying store items
    private RadioGroup radioGroupStoreMode;  // RadioGroup for selecting store mode (manual entry or select from server)
    private View layoutManualEntry;  // View for the manual entry layout
    private View layoutStoreList;  // View for the store list layout

    // Adapter and data variables for managing store data
    private StoreSelectionAdapter storeAdapter;  // Adapter for the RecyclerView to display store items
    private List<SavedLocation> storeList = new ArrayList<>();  // List to hold store data for display
    private TextInputEditText editTextSearchStores;  // Input field for searching stores
    private ViewSwitcher viewSwitcher;  // ViewSwitcher to toggle between manual entry and store list views

    private ArrayList<String> selectedIngredients;  // List to hold selected ingredients for filtering stores

    private FusedLocationProviderClient fusedLocationClient;  // FusedLocationClient to get current location
    private View view;  // Root view for the fragment layout
    private List<SavedLocation> masterStoreList = new ArrayList<>();  // List to store all stores, including unfiltered stores

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;  // Request code for location permission

    // Factory method to create a new instance of the fragment with selected ingredients
    public static StoreSelectionFragment newInstance(ArrayList<String> selectedIngredients) {
        StoreSelectionFragment fragment = new StoreSelectionFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("selectedIngredients", selectedIngredients);  // Passing selected ingredients as arguments
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // Calling the superclass onCreate method
        // Initialize selectedIngredients from the arguments, or create an empty list if not available
        selectedIngredients = getArguments() != null ?
                getArguments().getStringArrayList("selectedIngredients") :
                new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for the fragment
        view = inflater.inflate(R.layout.fragment_store_selection, container, false);

        // Initialize UI components from the inflated layout
        recyclerViewStores = view.findViewById(R.id.recyclerview_stores);  // RecyclerView for store items
        editTextSearchStores = view.findViewById(R.id.edittext_search_stores);  // Search input field for stores
        radioGroupStoreMode = view.findViewById(R.id.radiogroup_store_mode);  // RadioGroup for store mode selection
        viewSwitcher = view.findViewById(R.id.view_switcher);  // ViewSwitcher to toggle between manual entry and store list

        // Set up the RecyclerView with a LinearLayoutManager and StoreSelectionAdapter
        recyclerViewStores.setLayoutManager(new LinearLayoutManager(requireContext()));  // Use LinearLayoutManager for vertical scrolling
        storeAdapter = new StoreSelectionAdapter(requireContext(), storeList);  // Initialize adapter with store data
        recyclerViewStores.setAdapter(storeAdapter);  // Set the adapter for the RecyclerView

        // Initialize the FusedLocationProviderClient to get the current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Set up listener for radio group to toggle between manual entry and store list views
        radioGroupStoreMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_manual_entry) {
                viewSwitcher.setDisplayedChild(0);  // Show manual entry layout
            } else if (checkedId == R.id.radio_select_from_server) {
                viewSwitcher.setDisplayedChild(1);  // Show store list layout
            }
        });

        // Add a text change listener to the search field to filter stores based on search query
        editTextSearchStores.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStores(s.toString());  // Call filter method when text changes
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    // Reset to the full list when search text is cleared
                    storeAdapter.updateList(storeList);
                }
            }
        });

        // Load store data from Firebase Firestore
        loadFavStoreDataFromFirebase();

        return view;  // Return the inflated view
    }

    // Method to load favorite store data from Firebase Firestore
    private void loadFavStoreDataFromFirebase() {
        storeList.clear();  // Clear any existing store data
        // Check if location permission is granted before accessing location
        if (checkLocationPermission()) {
            // Get the last known location from the location client
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double currentLat = location.getLatitude();  // Get current latitude
                    double currentLng = location.getLongitude();  // Get current longitude

                    // Initialize Firebase Firestore instance
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("favstore")  // Reference to the "favstore" collection in Firestore
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                storeList.clear();  // Clear any previous store data
                                // Iterate through each document in the Firestore collection
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    String storeName = document.getString("storeName");  // Get store name
                                    String address = document.getString("address");  // Get store address
                                    String latLong = document.getString("latLong");  // Get lat/long string
                                    List<String> ingredients = (List<String>) document.get("ingredients");  // Get store ingredients list

                                    // Parse latitude and longitude from the latLong string
                                    String[] latLngParts = latLong.replace("Lat:", "").replace("Long:", "").split(",");
                                    double storeLat = Double.parseDouble(latLngParts[0].trim());  // Parse store latitude
                                    double storeLng = Double.parseDouble(latLngParts[1].trim());  // Parse store longitude
                                    String distance = calculateDistance(currentLat, currentLng, storeLat, storeLng);  // Calculate distance from current location

                                    // Calculate matched ingredients between store and selected ingredients
                                    List<String> matchedIngredients = new ArrayList<>();
                                    if (selectedIngredients != null) {
                                        for (String ingredient : ingredients) {
                                            for (String selectedItem : selectedIngredients) {
                                                if (ingredient.equalsIgnoreCase(selectedItem.trim())) {
                                                    matchedIngredients.add(ingredient);
                                                }
                                            }
                                        }
                                    }

                                    // Create a SavedLocation object for the store
                                    SavedLocation savedLocation = new SavedLocation(
                                            storeName,
                                            "https://example.com/default_image.jpg",  // Use a placeholder image
                                            address,
                                            storeLat,
                                            storeLng,
                                            distance,
                                            ingredients,
                                            matchedIngredients.size()  // Store the count of matched ingredients
                                    );

                                    savedLocation.setMatchedIngredients(matchedIngredients);  // Set matched ingredients list
                                    storeList.add(savedLocation);  // Add the store to the store list
                                    masterStoreList.add(savedLocation);  // Also add to the master store list
                                }
                                storeAdapter.notifyDataSetChanged();  // Notify the adapter to refresh the RecyclerView
                            })
                            .addOnFailureListener(e -> {
                                // Show an error message if loading data fails
                                showSnackbar("Failed to load data");
                                Log.e("StoreSelectionFragment", "Error loading data", e);
                            });
                }
            });
        }
    }

    // Method to calculate the distance between the current location and the store location
    private String calculateDistance(double currentLat, double currentLng, double storeLat, double storeLng) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLng, storeLat, storeLng, results);  // Calculate distance between two locations
        float distanceInMeters = results[0];  // Get the distance in meters

        // Return the formatted distance (in meters if less than 1 km, otherwise in kilometers)
        if (distanceInMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f m", distanceInMeters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000);
        }
    }

    // Method to filter the stores based on the search query
    private void filterStores(String query) {
        if (query.isEmpty()) {
            // If the search query is empty, show the full master store list
            storeAdapter.updateList(masterStoreList);
        } else {
            // Filter the stores based on the search query (case insensitive)
            List<SavedLocation> filteredList = new ArrayList<>();
            for (SavedLocation location : masterStoreList) {
                if (location.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(location);
                }
            }
            storeAdapter.updateList(filteredList);  // Update the adapter with the filtered list
        }
    }

    // Method to check if location permissions are granted
    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;  // Return true if location permissions are already granted
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Proceed with getting location if permission is granted
            } else {
                // Show a message if location permission is denied
                showSnackbar("Location permission denied. Please enable it in settings to get location.");
            }
        }
    }

    // Method to display a Snackbar message
    private void showSnackbar(String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();  // Show the Snackbar with the given message
    }

}
