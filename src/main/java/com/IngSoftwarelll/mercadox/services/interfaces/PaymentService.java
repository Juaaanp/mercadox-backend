package com.IngSoftwarelll.mercadox.services.interfaces;



import com.IngSoftwarelll.mercadox.dtos.payment.PaymentRequest;
import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PaymentService {

    /** Procesa una recarga de saldo */
    PaymentResponse pay(Long userId, PaymentRequest request);

    /** Historial de recargas del usuario */
    Page<PaymentResponse> getHistory(Long userId, Pageable pageable);
}