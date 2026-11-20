package com.hectorkaul.estoque.model;

import java.math.BigDecimal;

/** Um produto dentro do pedido de compra, com o custo combinado com o fornecedor. */
public record ItemCompra(long produtoId, String codigo, String nome, int quantidade, BigDecimal custoUnitario) {

    public BigDecimal subtotal() {
        return custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
