package com.IngSoftwarelll.mercadox.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseResponseDTO;
import com.IngSoftwarelll.mercadox.models.Purchase;

public interface PurchaseService {
    
    Purchase getPurchaseById(Long purchaseId);
    EntityCreatedResponseDTO createPurchase(Long userId, CreatePurchaseRequestDTO request);
    Page<PurchaseResponseDTO> getUserPurchases(Long userId, Pageable pageable);

}
