package com.project.ibm.retailmanagementsystem.dto;

import java.util.ArrayList;
import java.util.List;

/** Request body for {@code POST /store/placeOrder}, sent by the checkout form in the web UI. */
public class PlaceOrderRequestDTO {

    private String name;
    private String email;
    private String phone;
    private Long storeId;
    private Double totalPrice;
    private List<PurchaseProduct> purchaseProduct = new ArrayList<>();

    public PlaceOrderRequestDTO() {
    }

    public PlaceOrderRequestDTO(String name, String email, String phone, Long storeId,
                                Double totalPrice, List<PurchaseProduct> purchaseProduct) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.storeId = storeId;
        this.totalPrice = totalPrice;
        this.purchaseProduct = purchaseProduct;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<PurchaseProduct> getPurchaseProduct() {
        return purchaseProduct;
    }

    public void setPurchaseProduct(List<PurchaseProduct> purchaseProduct) {
        this.purchaseProduct = purchaseProduct;
    }
}
