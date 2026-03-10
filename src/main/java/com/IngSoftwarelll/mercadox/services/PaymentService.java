package com.IngSoftwarelll.mercadox.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.payment.PaymentRequest;
import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import com.IngSoftwarelll.mercadox.models.Payment;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.PaymentStatus;
import com.IngSoftwarelll.mercadox.repositories.PaymentRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final GatewaySimulatorService gateway;

    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest req) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Payment payment = Payment.builder()
            .user(user)
            .amount(req.amount())
            .paymentMethod(req.paymentMethod())
            .cardLastFour(req.cardLastFour())
            .status(PaymentStatus.PENDING)
            .build();

        try {
            String ref = gateway.process(req.amount(), req.cardLastFour());
            payment.setStatus(PaymentStatus.APPROVED);
            payment.setGatewayReference(ref);

            // Acreditar saldo al usuario
            user.setBalance(user.getBalance().add(req.amount()));
            userRepository.save(user);
            log.info("Transaccion aprovada");

        } catch (GatewaySimulatorService.PaymentDeclinedException ex) {
            payment.setStatus(PaymentStatus.DECLINED);
            payment.setGatewayReference("DECLINED-" + System.currentTimeMillis());
            log.warn("Transaccion declinada");
        }

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getHistory(Long userId, int page, int size) {
        return paymentRepository
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(PaymentResponse::from);
    }
}