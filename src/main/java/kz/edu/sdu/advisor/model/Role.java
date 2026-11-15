package kz.edu.sdu.advisor.model;

/**
 * Application user roles used for Spring Security RBAC (T-06).
 */
public enum Role {
    STUDENT,
    ADVISOR,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
