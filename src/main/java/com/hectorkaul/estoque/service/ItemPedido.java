package com.hectorkaul.estoque.service;

/** Codigo do produto e quantidade. Usado na venda (caixa) e no pedido de compra. */
public record ItemPedido(String codigoProduto, int quantidade) {
}
