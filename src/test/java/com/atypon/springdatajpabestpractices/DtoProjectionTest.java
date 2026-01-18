package com.atypon.springdatajpabestpractices;

import com.atypon.springdatajpabestpractices.entities.dtoProjectionIssue.*;
import com.atypon.springdatajpabestpractices.repositroy.dtoProjectionIssue.EmployeeRepository;
import com.atypon.springdatajpabestpractices.util.SQLInterceptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO Projection Problem: Loading entire entities when you only need a few columns.
 *
 * Example: For a dropdown list showing employee names, you fetch ALL 17 columns
 * including large BLOB (profilePicture) and CLOB (biography) fields!
 *
 * Solution: Use DTO projections to fetch only the columns you need.
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DtoProjectionTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setup() {
        for (int i = 1; i <= 5; i++) {
            Employee emp = new Employee();
            emp.setFirstName("John" + i);
            emp.setLastName("Doe" + i);
            emp.setEmail("john" + i + "@example.com");
            emp.setPhone("+1-555-000" + i);
            emp.setDepartment(i <= 3 ? "Engineering" : "Marketing");
            emp.setJobTitle("Developer");
            emp.setSalary(new BigDecimal("75000.00"));
            emp.setHireDate(LocalDate.of(2020, 1, i));
            emp.setBirthDate(LocalDate.of(1990, 6, i));
            emp.setAddress(i + " Main Street");
            emp.setCity("New York");
            emp.setCountry("USA");
            emp.setPostalCode("1000" + i);
            emp.setEmergencyContact("Jane Doe");
            emp.setEmergencyPhone("+1-555-999" + i);
            emp.setProfilePicture(new byte[1024 * 10]); // 10KB image
            emp.setBiography("A very long biography text... ".repeat(100)); // ~3KB text

            em.persist(emp);
        }

        em.flush();
        em.clear();
        SQLInterceptor.clear();
    }

    @Test
    @Order(1)
    @DisplayName("BAD: findAll() fetches ALL 17 columns")
    void badFetchAllColumns() {
        SQLInterceptor.clear();

        // We just want to show employee names in a dropdown
        List<Employee> employees = employeeRepository.findAll();

        // We only use firstName and lastName
        for (Employee emp : employees) {
            System.out.println("Employee: " + emp.getFirstName() + " " + emp.getLastName());
        }

        SQLInterceptor.printQueries("BAD: findAll() - fetches ALL columns");

        // Check the SQL - it fetches all columns
        String sql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        System.out.println("\n=== PROBLEM ===");
        System.out.println("We only needed: firstName, lastName");
        System.out.println("But SQL fetched ALL columns including:");
        System.out.println("- profilePicture (BLOB - 10KB per row)");
        System.out.println("- biography (CLOB - 3KB per row)");
        System.out.println("- salary, birthDate, address, etc.\n");

        // Count columns in SELECT clause (rough check)
        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        assertEquals(1, selectCount, "Should execute 1 query");
        assertEquals(5, employees.size(), "Should have 5 employees");

        // Verify the query fetches unnecessary large columns
        assertTrue(sql.contains("biography") || sql.contains("profile_picture"),
                "BAD: Query fetches large columns we don't need");
    }

    @Test
    @Order(2)
    @DisplayName("GOOD: Interface projection fetches only needed columns")
    void goodInterfaceProjection() {
        SQLInterceptor.clear();

        // Using interface projection - only fetches id, firstName, lastName
        List<EmployeeNameProjection> employees = employeeRepository.findAllProjectedBy();

        for (EmployeeNameProjection emp : employees) {
            System.out.println("Employee: " + emp.getFullName());
        }

        SQLInterceptor.printQueries("GOOD: Interface projection - only 3 columns");

        String sql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        System.out.println("\n=== SOLUTION ===");
        System.out.println("SQL only fetches: id, first_name, last_name");
        System.out.println("No profilePicture, no biography, no salary!\n");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        assertEquals(1, selectCount, "Should execute 1 query");
        assertEquals(5, employees.size(), "Should have 5 employees");

        // Verify the query does NOT fetch large columns
        assertFalse(sql.contains("biography"),
                "GOOD: Query should NOT fetch biography");
        assertFalse(sql.contains("profile_picture"),
                "GOOD: Query should NOT fetch profile_picture");
        assertFalse(sql.contains("salary"),
                "GOOD: Query should NOT fetch salary");
    }

    @Test
    @Order(3)
    @DisplayName("GOOD: Class-based DTO projection with constructor")
    void goodDtoProjection() {
        SQLInterceptor.clear();

        // Using class-based DTO - explicit constructor in JPQL
        List<EmployeeSummaryDto> employees = employeeRepository.findAllSummaries();

        for (EmployeeSummaryDto emp : employees) {
            System.out.println(emp);
        }

        SQLInterceptor.printQueries("GOOD: Class-based DTO projection");

        String sql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        System.out.println("\n=== DTO PROJECTION ===");
        System.out.println("SQL: SELECT id, first_name, last_name, department");
        System.out.println("Only 4 columns instead of 17!\n");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        assertEquals(1, selectCount, "Should execute 1 query");
        assertEquals(5, employees.size(), "Should have 5 employees");

        // Verify data is correct
        assertTrue(employees.stream().anyMatch(e -> e.getDepartment().equals("Engineering")));
        assertTrue(employees.stream().anyMatch(e -> e.getDepartment().equals("Marketing")));

        // Verify query is optimized
        assertFalse(sql.contains("biography"), "Should NOT fetch biography");
        assertFalse(sql.contains("salary"), "Should NOT fetch salary");
    }

    @Test
    @Order(4)
    @DisplayName("GOOD: Contact projection fetches only contact info")
    void goodContactProjection() {
        SQLInterceptor.clear();

        // Only need contact information
        List<EmployeeContactProjection> contacts = employeeRepository.findAllContactsBy();

        for (EmployeeContactProjection contact : contacts) {
            System.out.println(contact.getFirstName() + " " + contact.getLastName() +
                    " - " + contact.getEmail() + " - " + contact.getPhone());
        }

        SQLInterceptor.printQueries("GOOD: Contact projection - only 5 columns");

        String sql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        assertEquals(1, selectCount, "Should execute 1 query");
        assertEquals(5, contacts.size(), "Should have 5 contacts");

        // Verify optimized query
        assertFalse(sql.contains("biography"), "Should NOT fetch biography");
        assertFalse(sql.contains("profile_picture"), "Should NOT fetch profile_picture");
        assertFalse(sql.contains("salary"), "Should NOT fetch salary");
        assertFalse(sql.contains("address"), "Should NOT fetch address");
    }

    @Test
    @Order(5)
    @DisplayName("COMPARISON: Entity vs Projection column count")
    void comparisonEntityVsProjection() {
        // Fetch as Entity
        SQLInterceptor.clear();
        List<Employee> entities = employeeRepository.findAll();
        String entitySql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        // Fetch as Projection
        SQLInterceptor.clear();
        List<EmployeeNameProjection> projections = employeeRepository.findAllProjectedBy();
        String projectionSql = SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .findFirst()
                .orElse("");

        // Count columns (approximate by counting commas)
        int entityColumns = entitySql.split(",").length;
        int projectionColumns = projectionSql.split(",").length;

        System.out.println("\n============================================");
        System.out.println("COMPARISON: Entity vs DTO Projection");
        System.out.println("============================================");
        System.out.println("Entity query columns:     ~" + entityColumns);
        System.out.println("Projection query columns: ~" + projectionColumns);
        System.out.println("Columns saved:            ~" + (entityColumns - projectionColumns));
        System.out.println("============================================");
        System.out.println("\nWith 10KB profilePicture + 3KB biography per row:");
        System.out.println("- 1000 employees as Entity:     ~13 MB transferred");
        System.out.println("- 1000 employees as Projection: ~50 KB transferred");
        System.out.println("============================================\n");

        assertTrue(entityColumns > projectionColumns,
                "Entity should fetch more columns than projection");
        assertEquals(5, entities.size());
        assertEquals(5, projections.size());
    }

    @Test
    @Order(6)
    @DisplayName("SUMMARY: DTO Projection Best Practices")
    void summary() {
        System.out.println("""

            WHY USE DTO PROJECTIONS?
            ========================

            1. PERFORMANCE
            --------------
            - Fetch only columns you need
            - Reduce network bandwidth
            - Lower memory usage
            - Faster query execution


            2. SECURITY
            -----------
            - Don't expose sensitive fields (salary, SSN)
            - API responses contain only needed data


            3. TYPES OF PROJECTIONS
            -----------------------

            A) Interface-based (Spring Data JPA native):

               public interface EmployeeNameProjection {
                   Long getId();
                   String getFirstName();
                   String getLastName();
               }

               // Repository
               List<EmployeeNameProjection> findAllProjectedBy();


            B) Class-based DTO (JPQL constructor):

               public record EmployeeDto(Long id, String name) {}

               @Query("SELECT new pkg.EmployeeDto(e.id, e.firstName) FROM Employee e")
               List<EmployeeDto> findAllDtos();


            C) Dynamic projection:

               <T> List<T> findByDepartment(String dept, Class<T> type);

               // Usage
               repo.findByDepartment("IT", EmployeeNameProjection.class);
               repo.findByDepartment("IT", EmployeeContactProjection.class);


            BEST PRACTICES:
            ===============

            1. Use projections for READ operations (lists, searches)
            2. Use entities for WRITE operations (create, update)
            3. Never fetch LOB columns (BLOB, CLOB) unless needed
            4. Create specific projections for each use case
            5. Consider using records for immutable DTOs (Java 16+)

            """);
    }
}
