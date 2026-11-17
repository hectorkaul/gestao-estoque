package com.hectorkaul.estoque.db;

import java.util.function.Supplier;

/**
 * Executa um bloco de codigo como uma unica operacao: ou grava tudo, ou nada.
 * Usado na venda, que mexe em estoque, movimentacao e venda ao mesmo tempo.
 */
public interface Transacao {

    <T> T executar(Supplier<T> bloco);

    default void executarSemRetorno(Runnable bloco) {
        executar(() -> {
            bloco.run();
            return null;
        });
    }
}
