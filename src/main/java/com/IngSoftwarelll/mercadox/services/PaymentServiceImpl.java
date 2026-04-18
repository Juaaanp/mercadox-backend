package com.IngSoftwarelll.mercadox.services;


import java.util.UUID;

import com.IngSoftwarelll.mercadox.dtos.payment.PaymentRequest;
import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.mappers.PaymentMapper;
import com.IngSoftwarelll.mercadox.models.BalanceRecharge;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.repositories.BalanceRechargeRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BalanceRechargeRepository rechargeRepository;
    private final UserRepository            userRepository;
    private final PaymentMapper             paymentMapper;

    @Override
    @Transactional
    public PaymentResponse pay(Long userId, PaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Simulación del gateway — montos múltiplo exacto de 100.000 son rechazados
        PurchaseStatus status = PurchaseStatus.COMPLETED;

        BalanceRecharge recharge = BalanceRecharge.builder()
                .user(user)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(status)
                .cardLastFour(request.cardLastFour())
                .cardHolder(request.cardHolder())
                .expiryDate(request.expiryDate())
                .gatewayReference(UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .build();

        rechargeRepository.save(recharge);

        // Solo acreditar al balance si fue aprobado
        if (status == PurchaseStatus.COMPLETED) {
            user.setBalance(user.getBalance().add(request.amount()));
            userRepository.save(user);
            log.info("Recarga aprobada para usuario {}. Monto: {}", userId, request.amount());
        } else {
            log.info("Recarga rechazada para usuario {}. Monto: {}", userId, request.amount());
        }

        return paymentMapper.toPaymentResponse(recharge);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getHistory(Long userId, Pageable pageable) {
        return rechargeRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(paymentMapper::toPaymentResponse);
    }

}