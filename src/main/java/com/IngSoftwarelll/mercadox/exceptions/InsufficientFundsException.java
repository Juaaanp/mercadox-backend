package com.IngSoftwarelll.mercadox.exceptions;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(){
        super("Insufficient Funds");
    }
}
