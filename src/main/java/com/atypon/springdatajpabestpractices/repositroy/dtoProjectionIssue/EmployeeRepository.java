package com.atypon.springdatajpabestpractices.repositroy.dtoProjectionIssue;

import com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // =====================================================
    // BAD: Fetches ALL 17 columns even if you only need 2
    // =====================================================

    // This loads: id, firstName, lastName, email, phone, department,
    // jobTitle, salary, hireDate, birthDate, address, city, country,
    // postalCode, emergencyContact, emergencyPhone, profilePicture, biography
    List<Employee> findAll();

    List<Employee> findByDepartment(String department);


    // =====================================================
    // GOOD: Interface-based projection - only needed columns
    // =====================================================

    // Only loads: id, first_name, last_name
    List<EmployeeNameProjection> findAllProjectedBy();

    // Only loads: id, first_name, last_name, email, phone
    List<EmployeeContactProjection> findAllContactsBy();

    // Dynamic projection - caller decides what to fetch
    <T> List<T> findByDepartment(String department, Class<T> projectionType);


    // =====================================================
    // GOOD: Class-based DTO projection with constructor
    // =====================================================

    @Query("""
        SELECT new com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue.EmployeeSummaryDto(
            e.id, e.firstName, e.lastName, e.department
        )
        FROM Employee e
        """)
    List<EmployeeSummaryDto> findAllSummaries();

    @Query("""
        SELECT new com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue.EmployeeSummaryDto(
            e.id, e.firstName, e.lastName, e.department
        )
        FROM Employee e
        WHERE e.department = :department
        """)
    List<EmployeeSummaryDto> findSummariesByDepartment(String department);
}
