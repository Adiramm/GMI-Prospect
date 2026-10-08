package com.gmi.prospect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gmi.prospect.R;
import com.gmi.prospect.base.BaseActivity;
import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Faculty;
import com.gmi.prospect.data.Level;
import com.gmi.prospect.ui.adapter.CourseAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Course list with three tabs (Diploma / GAPP / GUFP) filtering ONE RecyclerView.
 * <p>
 * Tab position = {@link Level#ordinal()}, so the tabs always follow the Level
 * enum. The Diploma tab is grouped by faculty with small section headers.
 * The selected tab survives rotation / theme / language changes through
 * onSaveInstanceState.
 */
public class CourseListActivity extends BaseActivity {

    /** Bundle key for the selected tab index. */
    private static final String KEY_SELECTED_TAB = "selected_tab";

    private TabLayout tabLevels;
    private RecyclerView recyclerCourses;
    private CourseAdapter adapter;

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

        int selectedTab = 0; // fresh start → Diploma
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            Log.d(TAG, "CourseListActivity restored tab " + selectedTab);
        }
        setupTabs(selectedTab);
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

    /** Puts the courses of one level into the RecyclerView. */
    private void showLevel(Level level) {
        adapter.setItems(buildRows(level));
    }

    /**
     * Builds the rows for one tab. For Diplomas a Faculty header is inserted
     * whenever the faculty changes (CourseRepository keeps diplomas grouped
     * by faculty). GAPP and GUFP have one course each, so no headers.
     */
    private List<Object> buildRows(Level level) {
        List<Object> rows = new ArrayList<>();
        Faculty currentFaculty = null;
        for (Course course : CourseRepository.getCoursesByLevel(level)) {
            if (level == Level.DIPLOMA && course.getFaculty() != currentFaculty) {
                currentFaculty = course.getFaculty();
                rows.add(currentFaculty);
            }
            rows.add(course);
        }
        return rows;
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
