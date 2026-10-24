package com.project.ibm.retailmanagementsystem.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.project.ibm.retailmanagementsystem.dto.PlaceOrderRequestDTO;
import com.project.ibm.retailmanagementsystem.model.OrderDetails;
import com.project.ibm.retailmanagementsystem.service.OrderException;
import com.project.ibm.retailmanagementsystem.service.OrderService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock private OrderService orderService;
    @InjectMocks private StoreController controller;

    @Test
    void returnsOkWhenOrderIsPlaced() {
        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        when(orderService.saveOrder(request)).thenReturn(new OrderDetails());

        ResponseEntity<Map<String, String>> response = controller.placeOrder(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Order placed successfully", response.getBody().get("message"));
    }

    @Test
    void returnsBadRequestWithReasonWhenOrderIsRejected() {
        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        when(orderService.saveOrder(request)).thenThrow(new OrderException("Not enough stock for Mug."));

        ResponseEntity<Map<String, String>> response = controller.placeOrder(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Not enough stock for Mug.", response.getBody().get("message"));
    }
}
