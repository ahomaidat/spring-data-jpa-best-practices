package com.atypon.springdatajpabestpractices;

import com.atypon.springdatajpabestpractices.entities.osivIssue.Order;
import com.atypon.springdatajpabestpractices.entities.osivIssue.OrderItem;
import com.atypon.springdatajpabestpractices.entities.osivIssue.ShippingDetails;
import com.atypon.springdatajpabestpractices.repositroy.osivIssue.OrderRepository;
import com.atypon.springdatajpabestpractices.service.osivIssue.OrderService;
import com.atypon.springdatajpabestpractices.util.SQLInterceptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OSIV DEMONSTRATION
 *
 * This test demonstrates why Open Session In View is dangerous by:
 * 1. Showing behavior WITH spring.jpa.open-in-view=false (current config)
 * 2. Manually simulating what OSIV does (keeps session open)
 * 3. Comparing both scenarios
 *
 * NOTE: OSIV requires actual HTTP servlet requests to work. In unit tests,
 * even with spring.jpa.open-in-view=true, the OpenEntityManagerInViewFilter
 * isn't active. So we manually demonstrate the behavior by controlling
 * the EntityManager lifecycle.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OSIVDemonstrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long orderId;

    @BeforeEach
    void setupTestData() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();

        Order order = new Order();
        order.setCustomerName("Test Customer");
        order.setOrderDate(LocalDateTime.now());

        OrderItem item1 = new OrderItem();
        item1.setProductName("Laptop");
        item1.setQuantity(1);
        item1.setPrice(1200.00);
        order.addItem(item1);

        OrderItem item2 = new OrderItem();
        item2.setProductName("Mouse");
        item2.setQuantity(2);
        item2.setPrice(25.00);
        order.addItem(item2);

        ShippingDetails shipping = new ShippingDetails();
        shipping.setAddress("123 Test St");
        shipping.setCity("Test City");
        shipping.setCountry("Test Country");
        shipping.setTrackingNumber("TEST123");
        order.setShippingDetails(shipping);

        em.persist(order);
        em.flush();
        orderId = order.getId();

        em.getTransaction().commit();
        em.close();

        SQLInterceptor.clear();
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.execute(status -> {
            orderRepository.deleteAll();
            return null;
        });
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Current Config: OSIV DISABLED (session closes with transaction)")
    void currentConfig_OSIVDisabled() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCENARIO 1: OSIV DISABLED");
        System.out.println("Property: spring.jpa.open-in-view=false");
        System.out.println("=".repeat(80));

        SQLInterceptor.clear();

        // Service method - transaction ends here
        Order order = orderService.getOrderBad(orderId);

        // This throws LazyInitializationException
        Exception exception = assertThrows(LazyInitializationException.class, () -> {
            int size = order.getItems().size();
        });

        System.out.println("Result: LazyInitializationException thrown");
        System.out.println("Message: " + exception.getMessage());

        SQLInterceptor.printQueries("Queries (all within transaction)");

        System.out.println("\nANALYSIS:");
        System.out.println("  - Session lifecycle: Tied to transaction");
        System.out.println("  - Connection held: Only during query");
        System.out.println("  - Lazy loading fails with LazyInitializationException");
        System.out.println("  - THIS IS THE CORRECT BEHAVIOR");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Simulated: OSIV ENABLED (session stays open after transaction)")
    void simulated_OSIVEnabled() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCENARIO 2: OSIV ENABLED (Simulated)");
        System.out.println("Property: spring.jpa.open-in-view=true");
        System.out.println("=".repeat(80));

        SQLInterceptor.clear();
        EntityManager em = entityManagerFactory.createEntityManager();

        Order order;
        em.getTransaction().begin();
        order = em.find(Order.class, orderId);
        em.getTransaction().commit();

        System.out.println("Transaction ended but session still open");

        // With session still open, this works!
        assertDoesNotThrow(() -> {
            int itemCount = order.getItems().size();
            System.out.println("Lazy load succeeded: " + itemCount + " items");

            for (OrderItem item : order.getItems()) {
                System.out.println("  - " + item.getProductName() + " $" + item.getPrice());
            }

            String city = order.getShippingDetails().getCity();
            System.out.println("  - Shipping city: " + city);
        });

        em.close();
        System.out.println("Session finally closed");

        SQLInterceptor.printQueries("Queries (some OUTSIDE transaction)");

        long selectCount = SQLInterceptor.getQueries().stream()
                .filter(q -> q.toLowerCase().startsWith("select"))
                .count();

        System.out.println("\nANALYSIS:");
        System.out.println("  - Session lifecycle: Entire HTTP request");
        System.out.println("  - Connection held: Entire request duration");
        System.out.println("  - Lazy loading works (no exception - BAD)");
        System.out.println("  - THIS IS THE ANTI-PATTERN");
        System.out.println("=".repeat(80) + "\n");

        assertTrue(selectCount > 1, "Should have N+1 queries");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("PROPER SOLUTION: DTO projection (best practice)")
    void properSolution_DTOProjection() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SCENARIO 3: PROPER SOLUTION");
        System.out.println("Using DTO projection - fetch only what you need");
        System.out.println("=".repeat(80));

        SQLInterceptor.clear();

        var orderSummary = orderService.getOrderSummary(orderId);

        System.out.println("Transaction ended, session closed");
        System.out.println("DTO contains only required data");

        assertDoesNotThrow(() -> {
            System.out.println("Customer: " + orderSummary.getCustomerName());
            System.out.println("Items: " + orderSummary.getItemCount());
            System.out.println("Total: $" + orderSummary.getTotalAmount());
            System.out.println("Shipping: " + orderSummary.getShippingCity());
        });

        SQLInterceptor.printQueries("Single optimized query");

        long selectCount = SQLInterceptor.getQueries().stream()
                .filter(q -> q.toLowerCase().startsWith("select"))
                .count();

        System.out.println("\nANALYSIS:");
        System.out.println("  - Single optimized query: " + selectCount);
        System.out.println("  - Fetches only required data");
        System.out.println("  - No entities loaded (just DTO)");
        System.out.println("  - No lazy loading possible");
        System.out.println("  - Connection held only during query");
        System.out.println("  - THIS IS THE BEST SOLUTION");
        System.out.println("=".repeat(80) + "\n");

        assertEquals(1, selectCount);
    }

}
