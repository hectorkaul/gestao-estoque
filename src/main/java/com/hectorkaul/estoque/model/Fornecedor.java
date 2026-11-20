package com.hectorkaul.estoque.model;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;

public class Fornecedor {

    private Long id;
    private final String nome;
    private final String contato;
    private final int prazoEntregaDias;

    public Fornecedor(String nome, String contato, int prazoEntregaDias) {
        this(null, nome, contato, prazoEntregaDias);
    }

    public Fornecedor(Long id, String nome, String contato, int prazoEntregaDias) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do fornecedor e obrigatorio.");
        }
        if (prazoEntregaDias < 0) {
            throw new RegraDeNegocioException("O prazo de entrega nao pode ser negativo.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.contato = contato == null ? "" : contato.trim();
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getContato() {
        return contato;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    @Override
    public String toString() {
        return "#" + id + " " + nome;
    }
}
