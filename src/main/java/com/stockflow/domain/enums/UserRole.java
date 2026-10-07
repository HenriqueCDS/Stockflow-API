package com.stockflow.domain.enums;

/**
 * Papel do usuario dentro da casa (tenant). Nao ha papeis alem destes dois:
 * OWNER e quem criou a casa (unico por tenant hoje, sem promocao/transferencia);
 * MEMBER e qualquer outra pessoa convidada para a casa.
 */
public enum UserRole {
    OWNER,
    MEMBER
}
