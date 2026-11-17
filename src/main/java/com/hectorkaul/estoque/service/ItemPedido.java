package com.hectorkaul.estoque.service;

/** O que o vendedor digita no caixa: codigo do produto e quantidade. */
public record ItemPedido(String codigoProduto, int quantidade) {
}
