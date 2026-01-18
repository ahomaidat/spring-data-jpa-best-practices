package com.atypon.springdatajpabestpractices.repositroy.osivIssue;

import com.atypon.springdatajpabestpractices.dto.osivIssue.OrderSummaryDTO;
import com.atypon.springdatajpabestpractices.entities.osivIssue.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // BAD: Returns lazy proxy - relies on OSIV to load associations
    Optional<Order> findById(Long id);

    // OK but not ideal: Eagerly fetches all associations
    // Only use when you actually need the full entity graph
    @EntityGraph(attributePaths = {"items", "shippingDetails"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(Long id);

    // BAD: Returns lazy proxies
    List<Order> findAll();

    // OK but not ideal: Eagerly fetches all associations
    @EntityGraph(attributePaths = {"items", "shippingDetails"})
    @Query("SELECT o FROM Order o")
    List<Order> findAllWithDetails();

    // BEST: DTO projection - fetches only what you need in a single query
    @Query("""
        SELECT new com.atypon.springdatajpabestpractices.dto.osivIssue.OrderSummaryDTO(
            o.id,
            o.customerName,
            COUNT(i),
            SUM(i.price * i.quantity),
            s.city
        )
        FROM Order o
        LEFT JOIN o.items i
        LEFT JOIN o.shippingDetails s
        WHERE o.id = :id
        GROUP BY o.id, o.customerName, s.city
    """)
    Optional<OrderSummaryDTO> findOrderSummaryById(Long id);
}
