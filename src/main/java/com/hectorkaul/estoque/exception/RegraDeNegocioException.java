package com.hectorkaul.estoque.exception;

/**
 * Erro de regra do sistema (estoque insuficiente, codigo repetido etc).
 * A mensagem e mostrada direto para o usuario no menu.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
