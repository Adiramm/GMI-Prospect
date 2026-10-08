package com.gmi.prospect.data;

/**
 * SPM grades, best first. Because the order is fixed, "C or better" simply
 * means ordinal() &lt;= C.ordinal().
 */
public enum Grade {
    A_PLUS("A+"), A("A"), A_MINUS("A-"), B_PLUS("B+"), B("B"),
    C_PLUS("C+"), C("C"), D("D"), E("E"), G("G");

    private final String label;

    Grade(String label) {
        this.label = label;
    }

    /** Text shown on the grade chip, e.g. "A+". Same in every language. */
    public String getLabel() {
        return label;
    }

    /** Credit = C or better. */
    public boolean isCredit() {
        return ordinal() <= C.ordinal();
    }

    /** Pass = E or better. */
    public boolean isPass() {
        return ordinal() <= E.ordinal();
    }

    /** An "A" grade = A- or better. */
    public boolean isA() {
        return ordinal() <= A_MINUS.ordinal();
    }

    /**
     * Finds a Grade from its label ("A+", "B" ...). Used when grades travel
     * between Activities as a HashMap&lt;String, String&gt;.
     *
     * @return the matching Grade, or null if the label is unknown
     */
    public static Grade fromLabel(String label) {
        for (Grade grade : values()) {
            if (grade.label.equals(label)) {
                return grade;
            }
        }
        return null;
    }
}
