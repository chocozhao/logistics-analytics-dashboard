package com.logistics.dashboard.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}