package com.hectorkaul.estoque.model;

import java.math.BigDecimal;

public record ProdutoVendido(String codigo, String nome, int quantidade, BigDecimal faturamento) {
}
