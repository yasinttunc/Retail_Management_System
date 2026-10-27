package com.project.ibm.retailmanagementsystem.service;

import com.project.ibm.retailmanagementsystem.dto.PlaceOrderRequestDTO;
import com.project.ibm.retailmanagementsystem.dto.PurchaseProduct;
import com.project.ibm.retailmanagementsystem.model.Customer;
import com.project.ibm.retailmanagementsystem.model.Inventory;
import com.project.ibm.retailmanagementsystem.model.OrderDetails;
import com.project.ibm.retailmanagementsystem.model.OrderItem;
import com.project.ibm.retailmanagementsystem.model.Product;
import com.project.ibm.retailmanagementsystem.model.Store;
import com.project.ibm.retailmanagementsystem.repository.CustomerRepository;
import com.project.ibm.retailmanagementsystem.repository.InventoryRepository;
import com.project.ibm.retailmanagementsystem.repository.OrderDetailsRepository;
import com.project.ibm.retailmanagementsystem.repository.OrderItemRepository;
import com.project.ibm.retailmanagementsystem.repository.ProductRepository;
import com.project.ibm.retailmanagementsystem.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places customer orders.
 *
 * <p>The whole order runs in one database transaction: every line is validated (product
 * stocked in the store, enough units available) before anything is written, and if any write
 * fails the customer, order header, stock changes and order lines are all rolled back. Prices
 * and the order total come from the product catalogue, not from the client.</p>
 */
@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        CustomerRepository customerRepository,
                        StoreRepository storeRepository,
                        OrderDetailsRepository orderDetailsRepository,
                        OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
        this.storeRepository = storeRepository;
        this.orderDetailsRepository = orderDetailsRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderDetails saveOrder(PlaceOrderRequestDTO request) {
        if (request == null || request.getPurchaseProduct() == null || request.getPurchaseProduct().isEmpty()) {
            throw new OrderException("An order must contain at least one product.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new OrderException("Customer email is required.");
        }
        if (request.getStoreId() == null) {
            throw new OrderException("Store is required.");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new OrderException("Store " + request.getStoreId() + " not found."));

        // 1. Validate every line before writing anything.
        Map<Long, Integer> quantities = mergeQuantities(request.getPurchaseProduct());
        List<Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findByid(productId);
            if (product == null) {
                throw new OrderException("Product " + productId + " not found.");
            }
            Inventory inventory = inventoryRepository.findByProductIdandStoreId(productId, store.getId());
            if (inventory == null) {
                throw new OrderException(product.getName() + " is not stocked in this store.");
            }
            if (inventory.getStockLevel() < quantity) {
                throw new OrderException("Not enough stock for " + product.getName()
                        + ": requested " + quantity + ", available " + inventory.getStockLevel() + ".");
            }
            lines.add(new Line(product, inventory, quantity));
            total = total.add(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(quantity)));
        }

        // 2. Find or create the customer.
        Customer customer = customerRepository.findByEmail(request.getEmail());
        if (customer == null) {
            customer = customerRepository.save(new Customer(request.getName(), request.getEmail(), request.getPhone()));
        }

        // 3. Save the order header with the server-side total.
        OrderDetails order = orderDetailsRepository.save(new OrderDetails(customer, store, LocalDateTime.now(),
                total.setScale(2, RoundingMode.HALF_UP).doubleValue()));

        // 4. Decrement stock and save each order line.
        for (Line line : lines) {
            line.inventory().setStockLevel(line.inventory().getStockLevel() - line.quantity());
            inventoryRepository.save(line.inventory());
            orderItemRepository.save(new OrderItem(order, line.product(), line.quantity(), line.product().getPrice()));
        }
        return order;
    }

    /** Combines repeated product IDs and rejects missing or non-positive quantities. */
    private static Map<Long, Integer> mergeQuantities(List<PurchaseProduct> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PurchaseProduct item : items) {
            if (item == null || item.getProductId() == null) {
                throw new OrderException("Every order line needs a product.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException("Quantity must be at least 1.");
            }
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private record Line(Product product, Inventory inventory, int quantity) {
    }
}
