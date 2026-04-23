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

    @Column(name = "client_id", length = 20)
    private String clientId;

    @Column(name = "order_id", length = 50, unique = true)
    private String orderId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "promised_delivery_date", nullable = false)
    private LocalDate promisedDeliveryDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(length = 50)
    private String carrier;

    @Column(name = "origin_city", length = 100)
    private String originCity;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(length = 20)
    private String status;

    @Column(length = 50)
    private String sku;

    @Column(name = "product_category", length = 50)
    private String productCategory;

    private Integer quantity;

    @Column(name = "unit_price_usd", precision = 10, scale = 2)
    private BigDecimal unitPriceUsd;

    @Column(name = "order_value_usd", precision = 10, scale = 2)
    private BigDecimal orderValueUsd;

    @Column(name = "is_promo")
    private Boolean isPromo;

    @Column(name = "promo_discount_pct", precision = 5, scale = 2)
    private BigDecimal promoDiscountPct;

    @Column(length = 20)
    private String region;

    @Column(length = 30)
    private String warehouse;

    @Transient
    public Integer getDeliveryDays() {
        if (deliveryDate == null || orderDate == null) return null;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(orderDate, deliveryDate);
    }

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getPromisedDeliveryDate() { return promisedDeliveryDate; }
    public void setPromisedDeliveryDate(LocalDate promisedDeliveryDate) { this.promisedDeliveryDate = promisedDeliveryDate; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }

    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPriceUsd() { return unitPriceUsd; }
    public void setUnitPriceUsd(BigDecimal unitPriceUsd) { this.unitPriceUsd = unitPriceUsd; }

    public BigDecimal getOrderValueUsd() { return orderValueUsd; }
    public void setOrderValueUsd(BigDecimal orderValueUsd) { this.orderValueUsd = orderValueUsd; }

    public Boolean getIsPromo() { return isPromo; }
    public void setIsPromo(Boolean isPromo) { this.isPromo = isPromo; }

    public BigDecimal getPromoDiscountPct() { return promoDiscountPct; }
    public void setPromoDiscountPct(BigDecimal promoDiscountPct) { this.promoDiscountPct = promoDiscountPct; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }
}
