package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.repository.MovimentacaoRepository;
import com.hectorkaul.estoque.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class EstoqueService {

    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;
    private final Clock relogio;

    public EstoqueService(ProdutoRepository produtos, MovimentacaoRepository movimentacoes, Clock relogio) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
        this.relogio = relogio;
    }

    public Produto cadastrar(Produto produto) {
        produtos.buscarPorCodigo(produto.getCodigo()).ifPresent(existente -> {
            throw new RegraDeNegocioException("Ja existe um produto com o codigo " + existente.getCodigo() + ".");
        });
        return produtos.salvar(produto);
    }

    public Produto buscar(String codigo) {
        return produtos.buscarPorCodigo(codigo)
                .orElseThrow(() -> new RegraDeNegocioException("Produto " + codigo + " nao encontrado."));
    }

    public List<Produto> listar() {
        return produtos.listarTodos();
    }

    public List<Produto> pesquisar(String trechoDoNome) {
        return produtos.buscarPorNome(trechoDoNome);
    }

    public Produto registrarEntrada(String codigo, int quantidade, String observacao) {
        return registrarEntrada(codigo, quantidade, observacao, agora());
    }

    /** Entrada com data informada (ex: lancar uma nota que chegou ontem). */
    public Produto registrarEntrada(String codigo, int quantidade, String observacao, LocalDateTime dataHora) {
        Produto produto = buscar(codigo);
        produto.adicionarEstoque(quantidade);
        produtos.salvar(produto);
        movimentacoes.registrar(Movimentacao.entrada(produto.getId(), quantidade, dataHora, observacao));
        return produto;
    }

    /** Saida manual: perda, quebra, uso interno. Venda tem fluxo proprio no VendaService. */
    public Produto registrarSaida(String codigo, int quantidade, String observacao) {
        Produto produto = buscar(codigo);
        produto.removerEstoque(quantidade);
        produtos.salvar(produto);
        movimentacoes.registrar(Movimentacao.saida(produto.getId(), quantidade, agora(), observacao));
        return produto;
    }

    public void alterarPrecos(String codigo, BigDecimal custo, BigDecimal venda) {
        Produto produto = buscar(codigo);
        produto.alterarPrecos(custo, venda);
        produtos.salvar(produto);
    }

    /** Produtos no minimo ou abaixo, os que mais faltam primeiro. */
    public List<Produto> produtosParaRepor() {
        return produtos.listarTodos().stream()
                .filter(Produto::precisaRepor)
                .sorted(Comparator.comparingInt(Produto::quantidadeParaRepor).reversed()
                        .thenComparing(Produto::getNome))
                .toList();
    }

    public BigDecimal valorTotalEmEstoque() {
        return produtos.listarTodos().stream()
                .map(Produto::valorEmEstoque)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Movimentacao> historico(String codigo) {
        Produto produto = buscar(codigo);
        return movimentacoes.listarPorProduto(produto.getId());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
