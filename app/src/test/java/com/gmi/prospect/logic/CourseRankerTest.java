package com.gmi.prospect.logic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gmi.prospect.data.Course;
import com.gmi.prospect.data.CourseRepository;
import com.gmi.prospect.data.Level;

import org.junit.Test;

import java.util.List;

/** Local unit tests for {@link CourseRanker} (run with: gradlew test). */
public class CourseRankerTest {

    @Test
    public void scoreAnswers_addsOnePerChosenInterest() {
        // ELECTRICAL x2, MECHANICAL x0, DESIGN x1, IT x3
        int[] scores = CourseRanker.scoreAnswers(new int[]{0, 3, 3, 2, 0, 3});
        assertArrayEquals(new int[]{2, 0, 1, 3}, scores);
    }

    @Test
    public void allIt_softwareEngineeringRanksFirst() {
        int[] scores = {0, 0, 0, 6}; // answered "IT" to every question
        List<Course> ranked = CourseRanker.rank(CourseRepository.getAllCourses(), scores, Course::getId);

        // Software Engineering is IT only (average 6). Creative Multimedia,
        // Electronics (Computer) and Industrial Communication average 3.
        assertEquals("software_engineering", ranked.get(0).getId());
        assertEquals(6.0, CourseRanker.matchScore(ranked.get(0), scores), 0.001);
        assertEquals(3.0, CourseRanker.matchScore(ranked.get(1), scores), 0.001);
    }

    @Test
    public void tie_diplomaBeforeGappAndGufp_thenByName() {
        int[] scores = {0, 0, 0, 0}; // everything ties on score
        List<Course> ranked = CourseRanker.rank(CourseRepository.getAllCourses(), scores, Course::getId);

        int diplomaCount = CourseRepository.getCoursesByLevel(Level.DIPLOMA).size();
        for (int i = 0; i < ranked.size(); i++) {
            boolean isDiploma = ranked.get(i).getLevel() == Level.DIPLOMA;
            assertEquals("position " + i, i < diplomaCount, isDiploma);
        }
        // Within the same group: alphabetical (by ID here)
        for (int i = 1; i < diplomaCount; i++) {
            assertTrue(ranked.get(i - 1).getId().compareTo(ranked.get(i).getId()) < 0);
        }
        assertEquals("gapp", ranked.get(diplomaCount).getId());
        assertEquals("gufp", ranked.get(diplomaCount + 1).getId());
    }
}
