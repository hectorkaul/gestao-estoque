package com.hectorkaul.estoque.model;

import java.math.BigDecimal;

public record ItemVenda(long produtoId, String descricao, int quantidade, BigDecimal precoUnitario) {

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
