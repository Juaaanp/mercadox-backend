package com.IngSoftwarelll.mercadox.models.enums;


public enum TicketStatus {
    OPEN,             // Recién creado por el cliente
    VALIDATING,       // Admin revisando la incidencia
    RESOLVED,         // Resuelto (reemplazo o reembolso aplicado)
    REJECTED,         // Rechazado con justificación
    CLOSED_INVALID    // Cerrado como inválido (incidencia no aplica)
}