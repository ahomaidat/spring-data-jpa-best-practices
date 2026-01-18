package com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue;

/**
 * Class-based DTO projection - use with constructor expression in JPQL.
 *
 * GOOD: Only fetches the columns you specify in the query.
 * More explicit than interface projection.
 *
 * Usage in @Query:
 * SELECT new com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue.EmployeeSummaryDto(
 *     e.id, e.firstName, e.lastName, e.department
 * ) FROM Employee e
 */
public class EmployeeSummaryDto {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String department;

    public EmployeeSummaryDto(Long id, String firstName, String lastName, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartment() { return department; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "EmployeeSummaryDto{" +
                "id=" + id +
                ", name='" + getFullName() + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}
