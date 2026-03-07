package com.IngSoftwarelll.mercadox.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductCategoryResponseDTO;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/productCategories")
@RequiredArgsConstructor
public class ProductCategoryController {
    
    private final ProductCategoryService productCategoryService;

    @GetMapping
    public ResponseEntity<List<ProductCategoryResponseDTO>> getProductCategories (){
        return ResponseEntity.ok(productCategoryService.getProductCategories());
    }
}
