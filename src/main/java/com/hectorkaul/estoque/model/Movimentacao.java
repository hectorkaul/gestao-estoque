package com.hectorkaul.estoque.model;

import java.time.LocalDateTime;

public record Movimentacao(Long id,
                           long produtoId,
                           TipoMovimentacao tipo,
                           int quantidade,
                           LocalDateTime dataHora,
                           String observacao) {

    public static Movimentacao entrada(long produtoId, int quantidade, LocalDateTime dataHora, String observacao) {
        return new Movimentacao(null, produtoId, TipoMovimentacao.ENTRADA, quantidade, dataHora, observacao);
    }

    public static Movimentacao saida(long produtoId, int quantidade, LocalDateTime dataHora, String observacao) {
        return new Movimentacao(null, produtoId, TipoMovimentacao.SAIDA, quantidade, dataHora, observacao);
    }
}
