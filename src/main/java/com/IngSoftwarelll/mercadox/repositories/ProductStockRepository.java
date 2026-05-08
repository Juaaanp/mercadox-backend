package com.IngSoftwarelll.mercadox.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;


@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    @Query(value = "SELECT * FROM product_stock WHERE product_id = :productId AND status = 'AVAILABLE' ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<ProductStock> findNextAvailableForProduct(@Param("productId") Long productId);

    Page<ProductStock> findByProductIdAndCodeContainingIgnoreCase(
        Long productId, String code, Pageable pageable
    );
    
    Page<ProductStock> findByProductIdAndStatus(
        Long productId, StockStatus status, Pageable pageable
    );
    
    Page<ProductStock> findByProductIdAndCodeContainingIgnoreCaseAndStatus(
        Long productId, String code, StockStatus status, Pageable pageable
    );

    @Query("SELECT ps FROM ProductStock ps " +
           "WHERE ps.id = :stockId " +
           "AND ps.product.id = :productId " +
           "AND ps.product.admin.id = :adminId")
    Optional<ProductStock> findByIdAndProductIdAndProductAdminId(@Param("stockId") Long stockId, @Param("productId") Long productId, @Param("adminId") Long adminId);
    
    boolean existsByProductIdAndCode(Long productId, String code);
    
    Page<ProductStock> findByProductId(Long productId, Pageable pageable);
}


