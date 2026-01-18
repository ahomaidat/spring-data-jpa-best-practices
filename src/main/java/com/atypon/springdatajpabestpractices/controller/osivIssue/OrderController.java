package com.atypon.springdatajpabestpractices.controller.osivIssue;

import com.atypon.springdatajpabestpractices.entities.osivIssue.Order;
import com.atypon.springdatajpabestpractices.entities.osivIssue.OrderItem;
import com.atypon.springdatajpabestpractices.service.osivIssue.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * BAD: Lazy loading happens in controller (outside transaction)
     * With OSIV enabled: Works but keeps DB connection for entire request
     * With OSIV disabled: Throws LazyInitializationException
     */
    @GetMapping("/bad/{id}")
    public Map<String, Object> getOrderBad(@PathVariable Long id) {
        // Service method returns - transaction ends here
        Order order = orderService.getOrderBad(id);

        // These lazy loads happen OUTSIDE the @Transactional boundary!
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("customer", order.getCustomerName());

        // LAZY LOAD 1: Accessing items collection
        response.put("itemCount", order.getItems().size());

        // LAZY LOAD 2: Iterating through items
        double total = 0.0;
        for (OrderItem item : order.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        response.put("total", total);

        // LAZY LOAD 3: Accessing shipping details
        if (order.getShippingDetails() != null) {
            response.put("shippingCity", order.getShippingDetails().getCity());
        }

        return response;
    }

    /**
     * BEST: Returns DTO projection - no lazy loading possible
     * Single optimized query, works with or without OSIV
     */
    @GetMapping("/best/{id}")
    public Map<String, Object> getOrderBest(@PathVariable Long id) {
        // DTO projection - single query, no entities
        var orderSummary = orderService.getOrderSummary(id);

        // No lazy loading - DTO contains only required data
        Map<String, Object> response = new HashMap<>();
        response.put("id", orderSummary.getId());
        response.put("customer", orderSummary.getCustomerName());
        response.put("itemCount", orderSummary.getItemCount());
        response.put("total", orderSummary.getTotalAmount());
        response.put("shippingCity", orderSummary.getShippingCity());

        return response;
    }

    /**
     * OK: Eager fetching with @EntityGraph
     * Use only when you need the full entity graph
     */
    @GetMapping("/eager/{id}")
    public Map<String, Object> getOrderEager(@PathVariable Long id) {
        // All data eagerly fetched in service
        Order order = orderService.getOrderWithEagerFetch(id);

        // No lazy loading - data already loaded
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("customer", order.getCustomerName());
        response.put("itemCount", order.getItems().size());

        double total = 0.0;
        for (OrderItem item : order.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        response.put("total", total);

        if (order.getShippingDetails() != null) {
            response.put("shippingCity", order.getShippingDetails().getCity());
        }

        return response;
    }

    /**
     * BAD: N+1 problem when iterating
     */
    @GetMapping("/all-bad")
    public List<Map<String, Object>> getAllOrdersBad() {
        List<Order> orders = orderService.getAllOrdersBad();

        // Each iteration triggers lazy loads!
        return orders.stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("customer", order.getCustomerName());
                    map.put("itemCount", order.getItems().size());  // Lazy load!
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * OK: Eager fetching prevents N+1
     */
    @GetMapping("/all-eager")
    public List<Map<String, Object>> getAllOrdersEager() {
        List<Order> orders = orderService.getAllOrdersWithEagerFetch();

        // No lazy loads - data already loaded
        return orders.stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("customer", order.getCustomerName());
                    map.put("itemCount", order.getItems().size());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
