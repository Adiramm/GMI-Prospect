package com.gmi.prospect.data;

import androidx.annotation.Nullable;

import com.gmi.prospect.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hard-coded list of programmes currently offered by GMI (from gmi.edu.my,
 * Oct 2026). No internet or database is needed.
 */
public final class CourseRepository {

    private static final List<Course> COURSES = Collections.unmodifiableList(Arrays.asList(
            // ----- Diploma: Electrical Engineering -----
            diploma("mechatronics", R.string.course_mechatronics_name, R.string.course_mechatronics_desc,
                    Faculty.ELECTRICAL, false, R.drawable.ic_bolt,
                    Interest.ELECTRICAL, Interest.MECHANICAL),
            diploma("instrumentation_control", R.string.course_instrumentation_control_name,
                    R.string.course_instrumentation_control_desc,
                    Faculty.ELECTRICAL, false, R.drawable.ic_bolt,
                    Interest.ELECTRICAL),
            diploma("electronics_computer", R.string.course_electronics_computer_name,
                    R.string.course_electronics_computer_desc,
                    Faculty.ELECTRICAL, false, R.drawable.ic_bolt,
                    Interest.ELECTRICAL, Interest.IT),
            diploma("autotronics", R.string.course_autotronics_name, R.string.course_autotronics_desc,
                    Faculty.ELECTRICAL, false, R.drawable.ic_bolt,
                    Interest.ELECTRICAL, Interest.MECHANICAL),
            diploma("sustainable_energy", R.string.course_sustainable_energy_name,
                    R.string.course_sustainable_energy_desc,
                    Faculty.ELECTRICAL, false, R.drawable.ic_bolt,
                    Interest.ELECTRICAL),
            diploma("industrial_communication", R.string.course_industrial_communication_name,
                    R.string.course_industrial_communication_desc,
                    Faculty.ELECTRICAL, true, R.drawable.ic_bolt,
                    Interest.ELECTRICAL, Interest.IT),
            diploma("facilities_automation", R.string.course_facilities_automation_name,
                    R.string.course_facilities_automation_desc,
                    Faculty.ELECTRICAL, true, R.drawable.ic_bolt,
                    Interest.ELECTRICAL),

            // ----- Diploma: Mechanical Engineering -----
            diploma("industrial_design", R.string.course_industrial_design_name,
                    R.string.course_industrial_design_desc,
                    Faculty.MECHANICAL, false, R.drawable.ic_design,
                    Interest.DESIGN, Interest.MECHANICAL),
            diploma("industrial_quality", R.string.course_industrial_quality_name,
                    R.string.course_industrial_quality_desc,
                    Faculty.MECHANICAL, false, R.drawable.ic_build,
                    Interest.MECHANICAL),
            diploma("product_design", R.string.course_product_design_name,
                    R.string.course_product_design_desc,
                    Faculty.MECHANICAL, false, R.drawable.ic_design,
                    Interest.DESIGN, Interest.MECHANICAL),
            diploma("cnc_precision", R.string.course_cnc_precision_name, R.string.course_cnc_precision_desc,
                    Faculty.MECHANICAL, false, R.drawable.ic_build,
                    Interest.MECHANICAL),
            diploma("manufacturing", R.string.course_manufacturing_name, R.string.course_manufacturing_desc,
                    Faculty.MECHANICAL, true, R.drawable.ic_build,
                    Interest.MECHANICAL),
            diploma("tool_die", R.string.course_tool_die_name, R.string.course_tool_die_desc,
                    Faculty.MECHANICAL, true, R.drawable.ic_build,
                    Interest.MECHANICAL),

            // ----- Diploma: Computer & Information -----
            diploma("software_engineering", R.string.course_software_engineering_name,
                    R.string.course_software_engineering_desc,
                    Faculty.COMPUTER, false, R.drawable.ic_code,
                    Interest.IT),
            diploma("creative_multimedia", R.string.course_creative_multimedia_name,
                    R.string.course_creative_multimedia_desc,
                    Faculty.COMPUTER, false, R.drawable.ic_design,
                    Interest.DESIGN, Interest.IT),

            // ----- Pre-university pathways -----
            new Course("gapp", R.string.course_gapp_name, R.string.course_gapp_desc,
                    R.string.duration_gapp, Level.GAPP, Faculty.PRE_UNIVERSITY, RuleType.GAPP,
                    false, R.drawable.ic_flight,
                    Interest.ELECTRICAL, Interest.MECHANICAL),
            new Course("gufp", R.string.course_gufp_name, R.string.course_gufp_desc,
                    R.string.duration_gufp, Level.GUFP, Faculty.PRE_UNIVERSITY, RuleType.GUFP,
                    false, R.drawable.ic_school,
                    Interest.ELECTRICAL, Interest.MECHANICAL, Interest.IT)
    ));

    private CourseRepository() {
        // Static helper only - no instances.
    }

    /** Shortcut for a 3-year diploma (all diplomas share level, duration and rule). */
    private static Course diploma(String id, int nameRes, int descRes, Faculty faculty,
                                  boolean limitedSeats, int icon, Interest... interests) {
        return new Course(id, nameRes, descRes, R.string.duration_3_years, Level.DIPLOMA,
                faculty, RuleType.DIPLOMA_GENERAL, limitedSeats, icon, interests);
    }

    /** Every course, Diplomas first (grouped by faculty), then GAPP and GUFP. */
    public static List<Course> getAllCourses() {
        return COURSES;
    }

    /** Courses for one tab of the course list (Diploma / GAPP / GUFP). */
    public static List<Course> getCoursesByLevel(Level level) {
        List<Course> result = new ArrayList<>();
        for (Course course : COURSES) {
            if (course.getLevel() == level) {
                result.add(course);
            }
        }
        return result;
    }

    /** Looks up a course by ID (used when only the ID is passed in an Intent). */
    @Nullable
    public static Course findById(String id) {
        for (Course course : COURSES) {
            if (course.getId().equals(id)) {
                return course;
            }
        }
        return null;
    }
}
