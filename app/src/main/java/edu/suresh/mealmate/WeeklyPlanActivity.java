package edu.suresh.mealmate;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import edu.suresh.mealmate.adapters.WeeklyPlanAdapter;

public class WeeklyPlanActivity extends AppCompatActivity {

    private static final String DATE_FORMAT = "EEE, MMM dd"; // Constant for date format
    private TabLayout tabLayoutDays;
    private ViewPager2 viewPagerMeals;
    private WeeklyPlanAdapter weeklyPlanAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_plan);

        initializeViews();
        setupViewPager();
    }

    /**
     * Initializes the views used in the activity.
     */
    private void initializeViews() {
        tabLayoutDays = findViewById(R.id.tabLayoutDays);
        viewPagerMeals = findViewById(R.id.viewPagerMeals);
    }

    /**
     * Sets up the ViewPager with an adapter and attaches the TabLayout to it.
     */
    private void setupViewPager() {
        weeklyPlanAdapter = new WeeklyPlanAdapter(this);
        viewPagerMeals.setAdapter(weeklyPlanAdapter);

        new TabLayoutMediator(tabLayoutDays, viewPagerMeals, this::setTabText).attach();
    }

    /**
     * Sets the text for each tab based on the position.
     *
     * @param tab      The tab to set the text for.
     * @param position The position of the tab.
     */
    private void setTabText(TabLayout.Tab tab, int position) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, position);

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        tab.setText(dateFormat.format(calendar.getTime()));
    }
}
