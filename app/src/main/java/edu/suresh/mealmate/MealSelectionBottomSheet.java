package edu.suresh.mealmate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import edu.suresh.mealmate.adapters.MealSelectionAdapter;
import edu.suresh.mealmate.model.Recipe;

public class MealSelectionBottomSheet extends BottomSheetDialogFragment {

    // Declare the UI components
    private RecyclerView recyclerView;
    private MealSelectionAdapter adapter;
    private List<Recipe> recipeList;
    private List<Recipe> selectedMeals = new ArrayList<>();
    private SearchView searchView;
    private Button confirmButton;
    private TextView selectedCountText;
    private MealSelectionListener mealSelectionListener; // Listener for meal selection
    private String mealType;

    // Static method to create a new instance of the MealSelectionBottomSheet with required arguments
    public static MealSelectionBottomSheet newInstance(List<Recipe> recipes, String mealType) {
        MealSelectionBottomSheet fragment = new MealSelectionBottomSheet();
        Bundle args = new Bundle();
        // Pass recipes and mealType as arguments to the fragment
        args.putSerializable("recipeList", (ArrayList<Recipe>) recipes);
        args.putString("mealType", mealType);
        fragment.setArguments(args);
        return fragment;
    }

    // Setter for the MealSelectionListener, allows the activity to set the listener
    public void setMealSelectionListener(MealSelectionListener listener) {
        this.mealSelectionListener = listener; // Assign the listener to the class variable
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the arguments passed to the fragment
        recipeList = (List<Recipe>) getArguments().getSerializable("recipeList");
        mealType = getArguments().getString("mealType");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for the bottom sheet fragment
        View view = inflater.inflate(R.layout.fragment_meal_selection, container, false);

        // Bind UI components to their respective views
        recyclerView = view.findViewById(R.id.mealRecyclerView);
        searchView = view.findViewById(R.id.searchMeal);
        confirmButton = view.findViewById(R.id.confirmMealSelection);
        selectedCountText = view.findViewById(R.id.selectedCountText);

        // Log the recipe list for debugging purposes
        Log.d("BottomSheetRecipe", recipeList.toString());

        // Set up the RecyclerView with a vertical layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        // Set up the adapter for RecyclerView with the recipe list and selected meals list
        adapter = new MealSelectionAdapter(recipeList, selectedMeals, selectedCountText, mealType);
        recyclerView.setAdapter(adapter);

        // Set up search functionality to filter recipes based on query text
        setupSearchView();
        // Set up the confirm button to trigger meal selection confirmation
        setupConfirmButton();

        return view; // Return the inflated view
    }

    // Set up the search view to filter meals based on query text
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query); // Filter recipes when submit button is pressed
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText); // Filter recipes while typing in the search view
                return true;
            }
        });
    }

    // Set up the confirm button to notify the listener of selected meals
    private void setupConfirmButton() {
        confirmButton.setOnClickListener(v -> {
            if (mealSelectionListener != null) {
                // Trigger the listener method when the confirm button is clicked
                mealSelectionListener.onMealsSelected(selectedMeals, mealType);
            }
            dismiss(); // Dismiss the bottom sheet after confirming the selection
        });
    }

    // Define an interface for the activity to implement, allowing it to receive the selected meals
    public interface MealSelectionListener {
        void onMealsSelected(List<Recipe> selectedMeals, String mealType); // Callback method
    }
}
