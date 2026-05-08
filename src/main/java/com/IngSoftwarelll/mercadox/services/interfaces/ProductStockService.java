package com.IngSoftwarelll.mercadox.services.interfaces;

import java.util.List;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductStockResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.BulkStockResponseDTO;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductStockService {
    
    Page<ProductStockResponseDTO> getProductStock(Long productId, Long adminId, String search,
            StockStatus status, Pageable pageable);

    void deleteStockItem(Long productId, Long stockId, Long adminId);

    BulkStockResponseDTO addBulkStockItems(Long productId, Long commercialId,
            List<CreateProductStockRequestDTO> requests);
}
