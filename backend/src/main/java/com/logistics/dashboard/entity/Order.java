package com.logistics.dashboard.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "promised_delivery_date", nullable = false)
    private LocalDate promisedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 50)
    private String carrier;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "destination_state", length = 50)
    private String destinationState;

    @Column(name = "destination_region", length = 50)
    private String destinationRegion;

    @Column(name = "order_value", precision = 10, scale = 2)
    private BigDecimal orderValue;

    @Column(length = 50)
    private String sku;

    private Integer quantity;

    // Derived fields (not stored in database)
    @Transient
    public Boolean getIsDelayed() {
        if (actualDeliveryDate == null || promisedDeliveryDate == null) {
            return null;
        }
        return actualDeliveryDate.isAfter(promisedDeliveryDate);
    }

    @Transient
    public Integer getDeliveryDays() {
        if (actualDeliveryDate == null || orderDate == null) {
            return null;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(orderDate, actualDeliveryDate);
    }

    @Transient
    public Boolean getIsDelivered() {
        return "delivered".equals(status);
    }

    // Constructors
    public Order() {
    }

    public Order(Long id, LocalDate orderDate, LocalDate promisedDeliveryDate, LocalDate actualDeliveryDate,
                 String status, String carrier, String destinationCity, String destinationState,
                 String destinationRegion, BigDecimal orderValue, String sku, Integer quantity) {
        this.id = id;
        this.orderDate = orderDate;
        this.promisedDeliveryDate = promisedDeliveryDate;
        this.actualDeliveryDate = actualDeliveryDate;
        this.status = status;
        this.carrier = carrier;
        this.destinationCity = destinationCity;
        this.destinationState = destinationState;
        this.destinationRegion = destinationRegion;
        this.orderValue = orderValue;
        this.sku = sku;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getPromisedDeliveryDate() {
        return promisedDeliveryDate;
    }

    public void setPromisedDeliveryDate(LocalDate promisedDeliveryDate) {
        this.promisedDeliveryDate = promisedDeliveryDate;
    }

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public void setDestinationState(String destinationState) {
        this.destinationState = destinationState;
    }

    public String getDestinationRegion() {
        return destinationRegion;
    }

    public void setDestinationRegion(String destinationRegion) {
        this.destinationRegion = destinationRegion;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(BigDecimal orderValue) {
        this.orderValue = orderValue;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}