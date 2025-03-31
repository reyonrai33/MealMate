package edu.suresh.mealmate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import edu.suresh.mealmate.adapters.InstructionDetailAdapter;
import edu.suresh.mealmate.home.DashboardActivity;
import edu.suresh.mealmate.home.RecipeEditActivity;
import edu.suresh.mealmate.model.Recipe;

public class RecipeDetailActivity extends AppCompatActivity {

    private ViewPager2 instructionsViewPager;
    private MaterialButton prevButton, nextButton;
    private InstructionDetailAdapter instructionsAdapter;

    private ImageView recipeImage;
    private TextView recipeNameTv, cookTime, totalIngredients;
    private LinearLayout ingredientsContainer;
    private Recipe recipe;
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail); // Set the layout for this activity

        // Retrieve the Recipe object from the intent passed to this activity
        recipe = getIntent().getParcelableExtra("RECIPE");

        // Initialize UI elements
        initializeViews();

        // Initialize the custom progress dialog to show during Firestore operations
        customProgressDialog = new CustomProgressDialog(RecipeDetailActivity.this);

        // If the recipe data exists, load it into the UI
        if (recipe != null) {
            loadRecipeDetails();
        }

        // Set up the toolbar with menu actions
        setupToolbar();
    }

    /**
     * Initializes the views used in this activity.
     */
    private void initializeViews() {
        // Bind the views from XML to Java variables
        recipeImage = findViewById(R.id.recipeImage);
        recipeNameTv = findViewById(R.id.recipeName);
        cookTime = findViewById(R.id.cookTime);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
    }

    /**
     * Loads the recipe details (ingredients, instructions, etc.) into the UI.
     */
    private void loadRecipeDetails() {
        // Load ingredients and display them in the ingredients container
        loadIngredients(recipe.getIngredients());

        // Set up the ViewPager2 to show recipe instructions
        setupInstructionsViewPager();

        // If a valid image URL is present, load the image using Glide
        String imageUrl = recipe.getPhotoUrl();
        if (!imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(recipeImage);
        }

        // Set the recipe name and cooking time in the UI
        recipeNameTv.setText(recipe.getRecipeName());
        cookTime.setText(recipe.getCookTime() + " Minutes");

        // Set up navigation buttons for moving through instructions
        setupInstructionNavigation();
    }

    /**
     * Sets up the ViewPager2 to display the recipe's instructions.
     */
    private void setupInstructionsViewPager() {
        // Initialize the ViewPager2 and its adapter
        instructionsViewPager = findViewById(R.id.instructionsViewPager);
        instructionsAdapter = new InstructionDetailAdapter(this, recipe.getInstructions());
        instructionsViewPager.setAdapter(instructionsAdapter);
    }

    /**
     * Sets up the buttons to navigate through the recipe instructions.
     */
    private void setupInstructionNavigation() {
        // Initialize previous and next buttons
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);

        // Set up the previous button to go to the previous instruction
        prevButton.setOnClickListener(v -> {
            if (instructionsViewPager.getCurrentItem() > 0) {
                instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() - 1);
            }
        });

        // Set up the next button to go to the next instruction
        nextButton.setOnClickListener(v -> {
            if (instructionsViewPager.getCurrentItem() < instructionsAdapter.getItemCount() - 1) {
                instructionsViewPager.setCurrentItem(instructionsViewPager.getCurrentItem() + 1);
            }
        });
    }

    /**
     * Sets up the toolbar with the action menu.
     */
    private void setupToolbar() {
        // Get the toolbar and inflate the recipe menu
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu().clear(); // Clear any pre-existing menu items
        toolbar.inflateMenu(R.menu.recipe_menu); // Inflate the custom recipe menu

        // Set up the toolbar's menu item click listener
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_recipe) {
                openEditRecipe(); // Open recipe editing screen
                return true;
            } else if (item.getItemId() == R.id.action_del_recipe) {
                deleteRecipe(); // Delete the recipe if requested
                return true;
            }
            return false; // Return false if no action is triggered
        });
    }

    /**
     * Opens the RecipeEditActivity to allow the user to edit the recipe.
     */
    void openEditRecipe() {
        if (recipe != null) {
            // Create an Intent to open the RecipeEditActivity
            Intent intent = new Intent(RecipeDetailActivity.this, RecipeEditActivity.class);
            intent.putExtra("RECIPE", recipe); // Pass the recipe data to the next activity
            startActivity(intent); // Start the edit recipe activity
        } else {
            // If the recipe data is missing, show an error message
            showSnackbar("Error: Recipe data is missing!");
        }
    }

    /**
     * Deletes the recipe from Firestore.
     */
    void deleteRecipe() {
        // Show a confirmation dialog before proceeding with the deletion
        new MaterialAlertDialogBuilder(RecipeDetailActivity.this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this recipe?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show the progress dialog while performing the deletion
                    customProgressDialog.show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Get today's date in the required format (YYYY-MM-DD)
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayDate = dateFormat.format(new Date());

                    // Query the "meals" collection to check if the recipe is used in any meal plan
                    db.collection("meals")
                            .get()
                            .addOnCompleteListener(mealTask -> {
                                if (mealTask.isSuccessful() && !mealTask.getResult().isEmpty()) {
                                    boolean isScheduled = false;
                                    boolean isBeforeToday = false;

                                    // Loop through the meals to check if the recipe is scheduled
                                    for (DocumentSnapshot mealDoc : mealTask.getResult()) {
                                        String mealDate = mealDoc.getId(); // Get the date of the meal (YYYY-MM-DD)

                                        List<Long> breakfast = (List<Long>) mealDoc.get("Breakfast");
                                        List<Long> lunch = (List<Long>) mealDoc.get("Lunch");
                                        List<Long> dinner = (List<Long>) mealDoc.get("Dinner");

                                        // Check if the recipe is included in any meal category (breakfast, lunch, dinner)
                                        if ((breakfast != null && breakfast.contains(recipe.getTimestamp())) ||
                                                (lunch != null && lunch.contains(recipe.getTimestamp())) ||
                                                (dinner != null && dinner.contains(recipe.getTimestamp()))) {
                                            isScheduled = true;

                                            // If the meal date is before today, allow the deletion
                                            if (mealDate.compareTo(todayDate) < 0) {
                                                isBeforeToday = true;
                                            }
                                        }
                                    }

                                    // If the recipe is not scheduled or it's in past meals, proceed with deletion
                                    if (!isScheduled || isBeforeToday) {
                                        deleteRecipeFromFirestore(db);
                                    } else {
                                        // If the recipe is scheduled for today or later, show an error message
                                        customProgressDialog.dismiss();
                                        showSnackbar("You cannot delete this recipe because it's scheduled in a meal plan from today onwards.");
                                    }
                                } else {
                                    // If no meals are found, allow the deletion
                                    deleteRecipeFromFirestore(db);
                                }
                            })
                            .addOnFailureListener(e -> {
                                customProgressDialog.dismiss();
                                showSnackbar("Error checking meal plan: " + e.getMessage());
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Dismiss dialog if "No" is selected
                .show();
    }

    /**
     * Deletes the recipe from Firestore and updates the UI.
     */
    private void deleteRecipeFromFirestore(FirebaseFirestore db) {
        // Query the Firestore database to find the recipe document by its timestamp
        db.collection("recipes")
                .whereEqualTo("timestamp", recipe.getTimestamp())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // If the recipe is found, delete it from Firestore
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("recipes").document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        showSnackbar("Recipe deleted successfully!"); // Show success message
                                        customProgressDialog.dismiss(); // Dismiss the progress dialog
                                        navigateToDashboard(); // Navigate back to the Dashboard
                                    })
                                    .addOnFailureListener(e -> {
                                        showSnackbar("Error deleting document: " + e.getMessage()); // Show error message
                                        customProgressDialog.dismiss(); // Dismiss the progress dialog
                                    });
                        }
                    } else {
                        // If no recipe document is found, show an error message
                        showSnackbar("No document found with this timestamp.");
                        customProgressDialog.dismiss(); // Dismiss the progress dialog
                    }
                })
                .addOnFailureListener(e -> {
                    showSnackbar("Error fetching recipe for deletion: " + e.getMessage());
                    customProgressDialog.dismiss(); // Dismiss the progress dialog
                });
    }

    /**
     * Navigates the user back to the DashboardActivity after a successful deletion.
     */
    private void navigateToDashboard() {
        // Create an intent to navigate to the DashboardActivity
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("FRAGMENT_INDEX", 0); // Set the fragment index for the dashboard
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear the activity stack
        startActivity(intent); // Start the Dashboard activity
        finish(); // Finish the current activity
    }

    /**
     * Loads and displays the ingredients in a structured format using ChipGroup.
     */
    private void loadIngredients(Map<String, List<String>> ingredients) {
        ingredientsContainer.removeAllViews(); // Clear any existing ingredients

        // Iterate over the ingredient categories and display them
        for (Map.Entry<String, List<String>> entry : ingredients.entrySet()) {
            String category = entry.getKey(); // Get the ingredient category (e.g., Vegetables)
            List<String> ingredientList = entry.getValue(); // Get the list of ingredients for this category

            // Create a TextView for the category title
            TextView categoryTitle = new TextView(this);
            categoryTitle.setText(category);
            categoryTitle.setTextSize(16); // Set the font size
            categoryTitle.setTextColor(getResources().getColor(R.color.on_surface)); // Set the color
            categoryTitle.setPadding(0, 16, 0, 8); // Set padding for spacing
            ingredientsContainer.addView(categoryTitle); // Add category title to the container

            // Create a ChipGroup to hold the individual ingredients as chips
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Add a Chip for each ingredient
            for (String ingredient : ingredientList) {
                Chip chip = new Chip(this);
                chip.setText(ingredient); // Set the text of the chip to the ingredient name
                chip.setChipBackgroundColorResource(R.color.on_surface_variant); // Set chip background color
                chip.setTextColor(getResources().getColor(R.color.white)); // Set chip text color
                chip.setChipCornerRadius(12f); // Set chip corner radius
                chipGroup.addView(chip); // Add the chip to the chip group
            }

            // Add the ChipGroup to the ingredients container
            ingredientsContainer.addView(chipGroup);
        }
    }

    /**
     * Displays a Snackbar with a given message to the user.
     */
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.primary_variant)) // Set background color
                .setTextColor(getResources().getColor(R.color.white)) // Set text color
                .show(); // Display the snackbar
    }
}
