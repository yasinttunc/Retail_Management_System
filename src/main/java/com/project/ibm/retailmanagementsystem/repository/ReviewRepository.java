package com.project.ibm.retailmanagementsystem.repository;

import com.project.ibm.retailmanagementsystem.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review,String> {

    List<Review> findByStoreIdAndProductId(long storeId, long productId);
}
