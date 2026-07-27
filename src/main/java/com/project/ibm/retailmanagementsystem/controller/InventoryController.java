package com.project.ibm.retailmanagementsystem.controller;

import com.project.ibm.retailmanagementsystem.model.Inventory;
import com.project.ibm.retailmanagementsystem.model.Product;
import com.project.ibm.retailmanagementsystem.repository.InventoryRepository;
import com.project.ibm.retailmanagementsystem.repository.ProductRepository;
import com.project.ibm.retailmanagementsystem.service.ServiceClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ServiceClass serviceClass;

    @PutMapping
    public Map<String,String> updateInventory(@RequestBody Inventory inventory){
        try {
            if (serviceClass.validateProductId(inventory.getProduct().getId())) {
                inventoryRepository.save(inventory);
                return Map.of("message", "Successfully updated product");
            }
        }catch (DataIntegrityViolationException e){
             e.printStackTrace();
             return Map.of("message", "No data available");
        }catch (Exception e){
            e.printStackTrace();
            return Map.of("message", "No data available");
        }
        return Map.of("message", "No data available");
    }

    @PostMapping
    public Map<String,String> saveInventory(@RequestBody Inventory inventory){
        try {
            if(serviceClass.validateInventory(inventory)){
                inventoryRepository.save(inventory);
                return Map.of("message", "Successfully saved product");
            }

        }catch (DataIntegrityViolationException e){
             e.printStackTrace();
             return Map.of("message", "No data available");

        }catch (Exception e){
            e.printStackTrace();
            return Map.of("message", "No data available");
        }
        return Map.of("message", "No data available");
    }

    @GetMapping("/{storeID}")
    public Map<String, Object> getAllProducts(@PathVariable Long storeID){
        List<Map<String, Object>> products = inventoryRepository.findByStore_Id(storeID).stream()
                .map(inv -> {
                    Product p = inv.getProduct();
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", p.getId());
                    row.put("name", p.getName());
                    row.put("category", p.getCategory());
                    row.put("price", p.getPrice());
                    row.put("sku", p.getSku());
                    row.put("stockLevel", inv.getStockLevel());
                    return row;
                })
                .collect(Collectors.toList());
        return Map.of("products", products);
    }

    @GetMapping("/filter/{category}/{name}/{storeid}")
    public Map<String,Object> getProductName(@PathVariable String category, @PathVariable String name,
                                             @PathVariable long storeid){
        if (category.equals("null")){
            return Map.of("product", productRepository.findByNameLike(storeid,name));
        }
        else if(name.equals("null")){
            return Map.of("product", productRepository.findByCategoryAndStoreId(storeid,category));
        }
        return Map.of("product", productRepository.findByNameAndCategory(storeid, name, category));
    }

    @GetMapping("/search/{name}/{storeid}")
    public Map<String,Object> searchProduct(@PathVariable String name,@PathVariable Long storeid){
        return Map.of("products", productRepository.findByNameLike(storeid,name));
    }

    @DeleteMapping("/{id}")
    public Map<String,String> removeProduct(@PathVariable Long id){
        try{
            inventoryRepository.deleteByProductId(id);
            return Map.of("message","Successfully deleted product");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "Id " + id + " not present in database");
        }
    }

    @GetMapping("/validate/{quantity}/{storeId}/{productId}")
    public boolean validateQuantity(@PathVariable Integer quantity,@PathVariable Long storeId,@PathVariable Long productId){
        Inventory inventory = inventoryRepository.findByProductIdandStoreId(productId,storeId);
        if(Objects.isNull(inventory)){
            return false;
        }
        return inventory.getStockLevel()>=quantity;
    }



}








