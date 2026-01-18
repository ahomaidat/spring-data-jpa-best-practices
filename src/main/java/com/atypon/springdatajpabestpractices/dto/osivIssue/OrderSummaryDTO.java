package com.atypon.springdatajpabestpractices.dto.osivIssue;

public class OrderSummaryDTO {
    private final Long id;
    private final String customerName;
    private final int itemCount;
    private final double totalAmount;
    private final String shippingCity;

    public OrderSummaryDTO(Long id, String customerName, long itemCount, double totalAmount, String shippingCity) {
        this.id = id;
        this.customerName = customerName;
        this.itemCount = (int) itemCount;
        this.totalAmount = totalAmount;
        this.shippingCity = shippingCity;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    @Override
    public String toString() {
        return "OrderSummaryDTO{" +
                "id=" + id +
                ", customerName='" + customerName + '\'' +
                ", itemCount=" + itemCount +
                ", totalAmount=" + totalAmount +
                ", shippingCity='" + shippingCity + '\'' +
                '}';
    }
}
