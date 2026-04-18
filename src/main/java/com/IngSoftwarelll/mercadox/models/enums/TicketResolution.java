package com.IngSoftwarelll.mercadox.models.enums;


public enum TicketResolution {
    REPLACEMENT_SENT,    // Se asignó y notificó un nuevo código
    REFUND_PROCESSED,    // Se devolvió el dinero al balance del usuario
    REJECTED,            // Se rechazó con justificación enviada al cliente
    CLOSED_INVALID       // Se cerró como inválido sin acción
}
