package com.atypon.springdatajpabestpractices.service.osivIssue;

import com.atypon.springdatajpabestpractices.dto.osivIssue.OrderSummaryDTO;
import com.atypon.springdatajpabestpractices.entities.osivIssue.Order;
import com.atypon.springdatajpabestpractices.repositroy.osivIssue.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * BAD: Returns entity with lazy associations outside transaction boundary
     * With OSIV enabled: appears to work (hides the problem)
     * With OSIV disabled: LazyInitializationException when accessing lazy fields
     */
    @Transactional(readOnly = true)
    public Order getOrderBad(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    /**
     * OK: Eagerly fetches all needed associations within transaction
     * Use when you need the full entity graph
     */
    @Transactional(readOnly = true)
    public Order getOrderWithEagerFetch(Long id) {
        return orderRepository.findByIdWithDetails(id).orElse(null);
    }

    /**
     * BEST: Returns DTO with only needed data
     * Single optimized query, no lazy loading issues
     */
    @Transactional(readOnly = true)
    public OrderSummaryDTO getOrderSummary(Long id) {
        return orderRepository.findOrderSummaryById(id).orElse(null);
    }

    /**
     * BAD: Returns list with lazy associations
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersBad() {
        return orderRepository.findAll();
    }

    /**
     * OK: Eagerly fetches all associations
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersWithEagerFetch() {
        return orderRepository.findAllWithDetails();
    }
}
