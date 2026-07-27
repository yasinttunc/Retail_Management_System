package com.project.ibm.retailmanagementsystem.repository;

import com.project.ibm.retailmanagementsystem.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
}
