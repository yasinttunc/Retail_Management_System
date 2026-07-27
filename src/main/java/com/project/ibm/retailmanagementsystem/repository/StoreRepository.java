package com.project.ibm.retailmanagementsystem.repository;

import com.project.ibm.retailmanagementsystem.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store,Long> {

    Store findByid(Long id);

    @Query("Select i FROM Store i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :pname, '%'))")
    List<Store> findBySubName(String pName);

}
