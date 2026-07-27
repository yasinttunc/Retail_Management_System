package com.project.ibm.retailmanagementsystem.service;

import com.project.ibm.retailmanagementsystem.dto.PlaceOrderRequestDTO;
import com.project.ibm.retailmanagementsystem.dto.PurchaseProduct;
import com.project.ibm.retailmanagementsystem.model.Customer;
import com.project.ibm.retailmanagementsystem.model.Inventory;
import com.project.ibm.retailmanagementsystem.model.OrderDetails;
import com.project.ibm.retailmanagementsystem.model.OrderItem;
import com.project.ibm.retailmanagementsystem.model.Store;
import com.project.ibm.retailmanagementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private OrderDetailsRepository orderDetailsRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    public void saveOrder(PlaceOrderRequestDTO placeOrderRequest) throws Error {

        // 1. Find the customer by email, or create and save a new one
        Customer customer = customerRepository.findByEmail(placeOrderRequest.getEmail());
        if (customer == null) {
            customer = new Customer(placeOrderRequest.getName(),
                    placeOrderRequest.getEmail(),
                    placeOrderRequest.getPhone());
            customerRepository.save(customer);
        }

        // 2. Fetch the store, or fail if it doesn't exist
        Store store = storeRepository.findById(placeOrderRequest.getStoreId())
                .orElseThrow(() -> new Error("Store not found"));

        // 3. Create and save the order header
        OrderDetails orderDetails = new OrderDetails(customer, store,
                LocalDateTime.now(), placeOrderRequest.getTotalPrice());
        orderDetailsRepository.save(orderDetails);

        // 4. For each purchased product: decrement stock and save an order item
        for (PurchaseProduct product : placeOrderRequest.getPurchaseProduct()) {
            Inventory inventory = inventoryRepository.findByProductIdandStoreId(
                    product.getProductId(), placeOrderRequest.getStoreId());
            inventory.setStockLevel(inventory.getStockLevel() - product.getQuantity());
            inventoryRepository.save(inventory);

            OrderItem orderItem = new OrderItem(
                    orderDetails,
                    productRepository.findByid(product.getProductId()),
                    product.getQuantity(),
                    product.getPrice());
            orderItemRepository.save(orderItem);
        }
    }






}
