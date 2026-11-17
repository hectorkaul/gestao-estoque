package com.hectorkaul.estoque.model;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Produto {

    private Long id;
    private final String codigo;
    private String nome;
    private String categoria;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;
    private int estoqueAtual;
    private int estoqueMinimo;

    public Produto(String codigo, String nome, String categoria,
                   BigDecimal precoCusto, BigDecimal precoVenda, int estoqueMinimo) {
        this(null, codigo, nome, categoria, precoCusto, precoVenda, 0, estoqueMinimo);
    }

    public Produto(Long id, String codigo, String nome, String categoria,
                   BigDecimal precoCusto, BigDecimal precoVenda, int estoqueAtual, int estoqueMinimo) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("O codigo do produto e obrigatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do produto e obrigatorio.");
        }
        if (precoCusto == null || precoCusto.signum() < 0) {
            throw new RegraDeNegocioException("Preco de custo invalido.");
        }
        if (precoVenda == null || precoVenda.signum() <= 0) {
            throw new RegraDeNegocioException("Preco de venda precisa ser maior que zero.");
        }
        if (estoqueAtual < 0 || estoqueMinimo < 0) {
            throw new RegraDeNegocioException("Estoque nao pode ser negativo.");
        }

        this.id = id;
        this.codigo = codigo.trim().toUpperCase();
        this.nome = nome.trim();
        this.categoria = (categoria == null || categoria.isBlank()) ? "Geral" : categoria.trim();
        this.precoCusto = precoCusto.setScale(2, RoundingMode.HALF_UP);
        this.precoVenda = precoVenda.setScale(2, RoundingMode.HALF_UP);
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void adicionarEstoque(int quantidade) {
        validarQuantidade(quantidade);
        estoqueAtual += quantidade;
    }

    public void removerEstoque(int quantidade) {
        validarQuantidade(quantidade);
        if (quantidade > estoqueAtual) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente de " + nome + ": tem " + estoqueAtual + ", pediu " + quantidade + ".");
        }
        estoqueAtual -= quantidade;
    }

    public boolean precisaRepor() {
        return estoqueAtual <= estoqueMinimo;
    }

    public int quantidadeParaRepor() {
        return Math.max(0, estoqueMinimo - estoqueAtual);
    }

    /** Margem sobre o custo, em %. Ex: custo 10, venda 15 -> 50.0 */
    public BigDecimal margemPercentual() {
        if (precoCusto.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return precoVenda.subtract(precoCusto)
                .multiply(BigDecimal.valueOf(100))
                .divide(precoCusto, 1, RoundingMode.HALF_UP);
    }

    public BigDecimal valorEmEstoque() {
        return precoCusto.multiply(BigDecimal.valueOf(estoqueAtual));
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade precisa ser maior que zero.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void alterarPrecos(BigDecimal novoCusto, BigDecimal novaVenda) {
        if (novoCusto.signum() < 0 || novaVenda.signum() <= 0) {
            throw new RegraDeNegocioException("Precos invalidos.");
        }
        this.precoCusto = novoCusto.setScale(2, RoundingMode.HALF_UP);
        this.precoVenda = novaVenda.setScale(2, RoundingMode.HALF_UP);
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto outro)) return false;
        return codigo.equals(outro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + nome;
    }
}
