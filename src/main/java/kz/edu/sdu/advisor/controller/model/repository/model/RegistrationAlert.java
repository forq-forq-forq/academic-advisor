package kz.edu.sdu.advisor.model;

public record RegistrationAlert(String periodName, long daysUntilOpen, int cartSize) {

    public String message() {
        String when = daysUntilOpen == 0 ? "today"
                    : daysUntilOpen == 1 ? "tomorrow"
                    : "in " + daysUntilOpen + " days";
        String courses = cartSize == 1 ? "course" : "courses";
        return "Registration opens " + when + " — your cart has " + cartSize + " " + courses;
    }
}
