package com.IngSoftwarelll.mercadox.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.ProductCategory;


@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long>{

    @Query("SELECT p FROM ProductCategory p")
    List<ProductCategory> findProductCategories();
}
