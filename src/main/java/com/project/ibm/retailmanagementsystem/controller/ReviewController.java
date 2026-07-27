package com.project.ibm.retailmanagementsystem.controller;

import com.project.ibm.retailmanagementsystem.model.Customer;
import com.project.ibm.retailmanagementsystem.model.Review;
import com.project.ibm.retailmanagementsystem.repository.CustomerRepository;
import com.project.ibm.retailmanagementsystem.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/{storeId}/{productId}")
    public Map<String, Object> getReview(@PathVariable long storeId, @PathVariable long productId){

        List<Review> reviewList = reviewRepository.findByStoreIdAndProductId(storeId,productId);
        //Now from all the recevied reviews filter remove the unwanted data and keep comment,
        // rating. Now add name of the customer to the table using customer id field in the reviews.
        List<Map<String, Object>> reviewsWithCustomerNames = new ArrayList<>();

        for (Review review : reviewList) {
            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("comment", review.getComment());
            reviewMap.put("rating", review.getRating());

            Customer customer = customerRepository.findByid(review.getCustomerId());
            if (customer != null) {
                reviewMap.put("customerName", customer.getName());
            }else {
                reviewMap.put("customerName", "Unknown");
            }
            reviewsWithCustomerNames.add(reviewMap);
        }
        return Map.of("reviews", reviewsWithCustomerNames);
    }
    @GetMapping
    public Map<String,Object> getAllReviews(){
        Map<String,Object> map = new HashMap<>();
        map.put("reviews",reviewRepository.findAll());
        return map;
    }
}
