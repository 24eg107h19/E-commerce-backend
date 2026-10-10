package com.ecommerce.backend.dto;

public class OrderRequest {
    private Long userId;
    private String shippingAddress;
    private String phoneNumber;

    public OrderRequest() {}

    public OrderRequest(Long userId, String shippingAddress, String phoneNumber) {
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
