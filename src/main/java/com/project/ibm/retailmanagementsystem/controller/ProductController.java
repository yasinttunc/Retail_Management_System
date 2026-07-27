package com.project.ibm.retailmanagementsystem.controller;

import com.project.ibm.retailmanagementsystem.model.Product;
import com.project.ibm.retailmanagementsystem.repository.InventoryRepository;
import com.project.ibm.retailmanagementsystem.repository.OrderItemRepository;
import com.project.ibm.retailmanagementsystem.repository.ProductRepository;
import com.project.ibm.retailmanagementsystem.service.ServiceClass;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ServiceClass serviceClass;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @PostMapping
    public Map<String,String> addProduct(@RequestBody Product product){
        try {
            if(productRepository.findByName(product.getName())==null){
                productRepository.save(product);
                return Map.of("message","Successfully saved product");
            }
            else{
                return Map.of("message","Product already present");
            }
        }catch (DataIntegrityViolationException e){
            e.printStackTrace();
            return Map.of("message","No data available");
        }catch (Exception e){
            e.printStackTrace();
            return Map.of("message","No data available");
        }
    }

    @GetMapping("/product/{id}")
    public Map<String,Object> getProductById(@PathVariable Long id){
        try {
            if (productRepository.findById(id)!=null){
                return Map.of("product",productRepository.findById(id));
            }
        }catch (Exception e){
            e.printStackTrace();
            return Map.of("message","No data available");
        }

        return Map.of("message","No data available");
    }
    @PutMapping()
    public Map<String,String> updateProduct(@RequestBody Product product){
        try {
            if (serviceClass.validateProductId(product.getId())) {
                productRepository.save(product);
                return Map.of("message", "Successfully updated product");
            }
        }catch (DataIntegrityViolationException e){
            e.printStackTrace();
            return Map.of("message","No data available");
        }
        return Map.of("message","No data available");
    }

    @GetMapping("/category/{name}/{category}")
    public Map<String,Object> filterbyCategoryProduct(@PathVariable String name, @PathVariable String category){

        if (name.equals("null")){
            return Map.of("products",productRepository.findByCategory(category));
        }

        else if (category.equals("null")){
            return Map.of("products",productRepository.findProductBySubName(name));
        }

        else{
            return Map.of("products",productRepository.findProductBySubNameAndCategory(name,category));
        }
    }
    @GetMapping("/all")
    public Map<String,Object> listProducts(){
        return Map.of("products",productRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public Map<String,String> deleteProduct(@PathVariable Long id){
        try{
            if (!serviceClass.validateProductId(id)) {
                return Map.of("message","Id " + id + " not present in database");
            }

            inventoryRepository.deleteByProductId(id);
            orderItemRepository.deleteById(id);
            productRepository.deleteById(id);
            return Map.of("message","Successfully deleted product");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "Id " + id + " not present in database");
        }
    }
    @GetMapping("/searchProduct/{name}")
    public Map<String,Object> searchProduct(@PathVariable String name){
        return Map.of("message",productRepository.findProductBySubName(name));
    }
}
