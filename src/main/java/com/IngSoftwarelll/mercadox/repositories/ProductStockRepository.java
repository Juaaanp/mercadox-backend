package com.IngSoftwarelll.mercadox.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.ProductStock;


@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    @Query(value = "SELECT * FROM product_stock WHERE product_id = :productId AND status = 'AVAILABLE' ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<ProductStock> findNextAvailableForProduct(@Param("productId") Long productId);
    
}

