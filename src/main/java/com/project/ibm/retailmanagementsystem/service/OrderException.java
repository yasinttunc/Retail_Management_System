package com.project.ibm.retailmanagementsystem.service;

/** Thrown when an order cannot be placed; the message is safe to show to the customer. */
public class OrderException extends RuntimeException {

    public OrderException(String message) {
        super(message);
    }
}
