package com.project.ibm.retailmanagementsystem.controller;

import com.project.ibm.retailmanagementsystem.dto.PlaceOrderRequestDTO;
import com.project.ibm.retailmanagementsystem.model.Store;
import com.project.ibm.retailmanagementsystem.repository.StoreRepository;
import com.project.ibm.retailmanagementsystem.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/store")
public class StoreConroller {

    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private OrderService orderService;

    @PostMapping()
    public Map<String,Long> addStore(@RequestBody Store store){
        storeRepository.save(store);
        return Map.of("message", store.getId());
    }

    @GetMapping("/validate/{storeId}")
    public boolean validateStore(@PathVariable Long storeId){
        return storeRepository.findById(storeId).isPresent();
    }

    @PostMapping("/placeOrder")
    public Map<String,String> placeOrder(@RequestBody PlaceOrderRequestDTO placeOrderRequestDTO) {

        try {
            orderService.saveOrder(placeOrderRequestDTO);
            return Map.of("message", "Order placed successfully");
        } catch (Error e) {
            e.printStackTrace();
            return Map.of("Error", "" + e);

        }
    }

}

