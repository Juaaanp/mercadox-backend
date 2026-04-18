package com.IngSoftwarelll.mercadox.mappers;

import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import org.mapstruct.Mapper;


import com.IngSoftwarelll.mercadox.models.BalanceRecharge;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toPaymentResponse(BalanceRecharge recharge);
}

