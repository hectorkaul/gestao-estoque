package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.db.Transacao;
import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.ItemVenda;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.model.Venda;
import com.hectorkaul.estoque.repository.MovimentacaoRepository;
import com.hectorkaul.estoque.repository.ProdutoRepository;
import com.hectorkaul.estoque.repository.VendaRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendaService {

    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;
    private final VendaRepository vendas;
    private final Transacao transacao;
    private final Clock relogio;

    public VendaService(ProdutoRepository produtos, MovimentacaoRepository movimentacoes,
                        VendaRepository vendas, Transacao transacao, Clock relogio) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
        this.vendas = vendas;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public Venda registrarVenda(String cliente, List<ItemPedido> pedido) {
        return registrarVenda(cliente, pedido, LocalDateTime.now(relogio).withNano(0));
    }

    /**
     * Valida tudo antes de gravar qualquer coisa. Se um item nao tiver estoque,
     * a venda inteira e recusada (nao fica venda pela metade).
     */
    public Venda registrarVenda(String cliente, List<ItemPedido> pedido, LocalDateTime dataHora) {
        if (pedido == null || pedido.isEmpty()) {
            throw new RegraDeNegocioException("A venda precisa ter pelo menos um item.");
        }

        // se o mesmo produto foi digitado duas vezes, soma as quantidades
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedido item : pedido) {
            if (item.quantidade() <= 0) {
                throw new RegraDeNegocioException("Quantidade invalida para " + item.codigoProduto() + ".");
            }
            quantidades.merge(item.codigoProduto().trim().toUpperCase(), item.quantidade(), Integer::sum);
        }

        Map<Produto, Integer> itensValidados = new LinkedHashMap<>();
        quantidades.forEach((codigo, quantidade) -> {
            Produto produto = produtos.buscarPorCodigo(codigo)
                    .orElseThrow(() -> new RegraDeNegocioException("Produto " + codigo + " nao encontrado."));
            if (quantidade > produto.getEstoqueAtual()) {
                throw new RegraDeNegocioException("Estoque insuficiente de " + produto.getNome()
                        + ": tem " + produto.getEstoqueAtual() + ", pediu " + quantidade + ".");
            }
            itensValidados.put(produto, quantidade);
        });

        return transacao.executar(() -> {
            Venda venda = new Venda(null, dataHora, cliente);
            itensValidados.forEach((produto, quantidade) -> {
                produto.removerEstoque(quantidade);
                produtos.salvar(produto);
                venda.adicionarItem(new ItemVenda(produto.getId(), produto.getNome(), quantidade, produto.getPrecoVenda()));
            });
            vendas.salvar(venda);
            venda.getItens().forEach(item -> movimentacoes.registrar(
                    Movimentacao.saida(item.produtoId(), item.quantidade(), dataHora, "Venda #" + venda.getId())));
            return venda;
        });
    }

    public List<Venda> listar() {
        return vendas.listarTodas();
    }
}
