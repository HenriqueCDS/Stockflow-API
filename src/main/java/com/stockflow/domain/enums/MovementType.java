package com.stockflow.domain.enums;

public enum MovementType {
    ENTRY,      // entrada de estoque (ex.: confirmação de nota)
    USED,       // "Usei" — consumo em 1 toque
    DISCARDED,  // "Descartei" — descarte em 1 toque (venceu, estragou etc.)
    EXIT,       // saída manual genérica (ajuste em /stock-movements/adjust)
    ADJUSTMENT, // ajuste manual (ex.: correção de inventário)
    RETURN      // devolução
}
