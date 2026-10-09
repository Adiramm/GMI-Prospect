package com.gmi.prospect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.base.LifecycleLog;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Faculty;
import com.gmi.prospect.data.Level;
import com.gmi.prospect.ui.adapter.CourseAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Course list with three tabs (Diploma / GAPP / GUFP) filtering ONE RecyclerView.
 * <p>
 * Tab position = {@link Level#ordinal()}, so the tabs always follow the Level
 * enum. The Diploma tab is grouped by faculty with small section headers.
 * The selected tab survives rotation / theme / language changes through
 * onSaveInstanceState.
 * <p>
 * A search field above the tabs filters the open tab by course name. Its text
 * is restored after rotation AUTOMATICALLY (the EditText has an id) - a handy
 * contrast with the tab, which we must save ourselves.
 */
public class CourseListActivity extends BaseActivity {

    /** Bundle key for the selected tab index. */
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabLayout tabLevels;
    private RecyclerView recyclerCourses;
    private CourseAdapter adapter;
    private EditText editSearch;
    private TextView textNoMatch;

    // Builds the UI. If Android re-created us (rotation, dark mode, language),
    // savedInstanceState holds the tab the user had open, so we select it
    // again instead of jumping back to "Diploma".
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_list);
        setupToolbar(R.string.title_courses);

        recyclerCourses = findViewById(R.id.recyclerCourses);
        adapter = new CourseAdapter(this::openCourseDetail);
        recyclerCourses.setAdapter(adapter);
        editSearch = findViewById(R.id.editSearch);
        textNoMatch = findViewById(R.id.textNoMatch);

        int selectedTab = 0; // fresh start → Diploma
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            LifecycleLog.d("CourseListActivity restored tab " + selectedTab);
        }
        setupTabs(selectedTab);
        // The search text itself is put back by Android AFTER onCreate (in
        // onRestoreInstanceState); the TextWatcher below then re-filters the list.
        setupSearch();
    }

    // Called before the Activity may be destroyed and re-created. Tabs are
    // added in code, so TabLayout cannot remember the selection itself -
    // we store the index and read it back in onCreate.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, tabLevels.getSelectedTabPosition());
    }

    // ---------------------------------------------------------------------
    // Tabs
    // ---------------------------------------------------------------------

    /** Adds one tab per Level, selects {@code selectedTab} and shows its courses. */
    private void setupTabs(int selectedTab) {
        tabLevels = findViewById(R.id.tabLevels);
        Level[] levels = Level.values();
        for (int i = 0; i < levels.length; i++) {
            TabLayout.Tab tab = tabLevels.newTab().setText(levels[i].getLabelRes());
            tabLevels.addTab(tab, i == selectedTab);
        }
        // Fill the list BEFORE the first layout so RecyclerView can also
        // restore its own scroll position after rotation.
        showLevel(levels[selectedTab]);

        // Listener added last, so selecting the restored tab above does not
        // trigger it a second time.
        tabLevels.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showLevel(Level.values()[tab.getPosition()]);
                recyclerCourses.scrollToPosition(0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // nothing to do
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Tapping the open tab again scrolls back to the top
                recyclerCourses.smoothScrollToPosition(0);
            }
        });
    }

    /** Puts the courses of one level (matching the search) into the RecyclerView. */
    private void showLevel(Level level) {
        String query = searchText();
        List<Object> rows = buildRows(level, query);
        adapter.setItems(rows);

        boolean noMatch = rows.isEmpty();
        recyclerCourses.setVisibility(noMatch ? View.GONE : View.VISIBLE);
        textNoMatch.setVisibility(noMatch ? View.VISIBLE : View.GONE);
        if (noMatch) {
            textNoMatch.setText(getString(R.string.search_no_results, query));
        }
    }

    /**
     * Builds the rows for one tab: only courses whose name contains the search
     * text. For Diplomas a Faculty header is inserted whenever the faculty
     * changes (CourseRepository keeps diplomas grouped by faculty), so a header
     * only appears when at least one of its courses matches. GAPP and GUFP have
     * one course each, so no headers.
     */
    private List<Object> buildRows(Level level, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Object> rows = new ArrayList<>();
        Faculty currentFaculty = null;
        for (Course course : CourseRepository.getCoursesByLevel(level)) {
            // Match against the name in the CURRENT language (EN or BM)
            String name = getString(course.getNameRes()).toLowerCase(Locale.ROOT);
            if (!name.contains(needle)) {
                continue;
            }
            if (level == Level.DIPLOMA && course.getFaculty() != currentFaculty) {
                currentFaculty = course.getFaculty();
                rows.add(currentFaculty);
            }
            rows.add(course);
        }
        return rows;
    }

    // ---------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------

    /** The trimmed search text ("" when empty). */
    private String searchText() {
        return editSearch.getText() == null ? "" : editSearch.getText().toString().trim();
    }

    /** Re-filters while typing (the keyboard's Done key closes itself: actionDone). */
    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing to do
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Same text on every tab: switching tab keeps the filter
                showLevel(Level.values()[tabLevels.getSelectedTabPosition()]);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    /** Opens the detail screen. Only the course ID travels in the Intent. */
    private void openCourseDetail(@NonNull Course course) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.getId());
        startActivity(intent);
    }
}
