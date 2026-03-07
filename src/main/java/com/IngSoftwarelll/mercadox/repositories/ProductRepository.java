package com.IngSoftwarelll.mercadox.repositories;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


        @Query("SELECT p FROM Product p " +
                        "JOIN FETCH p.productCategory c ")
        Page<Product> findAllProducts(Pageable pageable);

        @Query("SELECT p FROM Product p " +
                        "JOIN FETCH p.productCategory c " +
                        "WHERE (:searchQuery IS NULL OR :searchQuery = '' OR " +
                        "     LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
                        "     LOWER(p.description) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
                        "AND (:productCategoryId IS NULL OR c.id = :productCategoryId) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
        Page<Product> searchProducts(
                        @Param("searchQuery") String searchQuery,
                        @Param("productCategoryId") Long productCategoryId,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

}
