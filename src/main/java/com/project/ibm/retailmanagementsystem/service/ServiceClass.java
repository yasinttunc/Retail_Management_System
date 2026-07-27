package com.project.ibm.retailmanagementsystem.service;

import com.project.ibm.retailmanagementsystem.model.Inventory;
import com.project.ibm.retailmanagementsystem.model.Product;
import com.project.ibm.retailmanagementsystem.model.Store;
import com.project.ibm.retailmanagementsystem.repository.InventoryRepository;
import com.project.ibm.retailmanagementsystem.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceClass {

    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProductRepository productRepository;

    public boolean validateInventory(Inventory inventory){
        Store store = inventory.getStore();
        Product product = inventory.getProduct();
        if(inventoryRepository.findByProductIdandStoreId(product.getId(),store.getId())==null){
            return true;
        }
        return false;

    }

    public boolean validateProduct(Product product){
        if(productRepository.findByName(product.getName())==null){
            return false;
        }
        return true;
    }

    public boolean validateProductId(Long productId){
        return productRepository.findById(productId).isPresent();
    }

    public Inventory getInventoryId(Inventory inventory){
        return inventoryRepository.findByProductIdandStoreId(inventory.getProduct().getId(),
                inventory.getStore().getId());

    }

}
