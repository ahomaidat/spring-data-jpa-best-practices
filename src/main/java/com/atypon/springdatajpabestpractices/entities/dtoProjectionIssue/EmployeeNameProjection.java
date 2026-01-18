package com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue;

/**
 * Interface-based projection - Spring Data JPA creates a proxy.
 *
 * GOOD: Only fetches the columns defined in the interface.
 * SQL will be: SELECT id, first_name, last_name FROM employee
 * Instead of: SELECT id, first_name, last_name, email, phone, ... (all 17 columns)
 */
public interface EmployeeNameProjection {

    Long getId();
    String getFirstName();
    String getLastName();

    // Computed property using SpEL
    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }
}
