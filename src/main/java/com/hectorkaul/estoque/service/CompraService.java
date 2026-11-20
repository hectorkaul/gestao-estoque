package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.db.Transacao;
import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.Fornecedor;
import com.hectorkaul.estoque.model.ItemCompra;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.PedidoCompra;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.model.StatusPedido;
import com.hectorkaul.estoque.repository.FornecedorRepository;
import com.hectorkaul.estoque.repository.MovimentacaoRepository;
import com.hectorkaul.estoque.repository.PedidoCompraRepository;
import com.hectorkaul.estoque.repository.ProdutoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fornecedores e pedidos de compra. O estoque so aumenta quando o pedido e recebido,
 * nao quando e feito: mercadoria pedida ainda nao esta na prateleira.
 */
public class CompraService {

    private final FornecedorRepository fornecedores;
    private final PedidoCompraRepository pedidos;
    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;
    private final Transacao transacao;
    private final Clock relogio;

    public CompraService(FornecedorRepository fornecedores, PedidoCompraRepository pedidos,
                         ProdutoRepository produtos, MovimentacaoRepository movimentacoes,
                         Transacao transacao, Clock relogio) {
        this.fornecedores = fornecedores;
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    public Fornecedor cadastrarFornecedor(Fornecedor fornecedor) {
        return fornecedores.salvar(fornecedor);
    }

    public List<Fornecedor> listarFornecedores() {
        return fornecedores.listarTodos();
    }

    public Fornecedor buscarFornecedor(long id) {
        return fornecedores.buscarPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Fornecedor #" + id + " nao encontrado."));
    }

    /** Pedido com o custo atual de cada produto. Produto repetido tem as quantidades somadas. */
    public PedidoCompra criarPedido(long fornecedorId, List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido de compra precisa ter pelo menos um item.");
        }
        Fornecedor fornecedor = buscarFornecedor(fornecedorId);

        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedido item : itens) {
            if (item.quantidade() <= 0) {
                throw new RegraDeNegocioException("Quantidade invalida para " + item.codigoProduto() + ".");
            }
            quantidades.merge(item.codigoProduto().trim().toUpperCase(), item.quantidade(), Integer::sum);
        }

        PedidoCompra pedido = PedidoCompra.novo(fornecedor, agora());
        quantidades.forEach((codigo, quantidade) -> {
            Produto produto = produtos.buscarPorCodigo(codigo)
                    .orElseThrow(() -> new RegraDeNegocioException("Produto " + codigo + " nao encontrado."));
            pedido.adicionarItem(new ItemCompra(produto.getId(), produto.getCodigo(), produto.getNome(),
                    quantidade, produto.getPrecoCusto()));
        });

        return transacao.executar(() -> pedidos.salvar(pedido));
    }

    /**
     * Monta o pedido a partir da lista de reposicao: compra o que falta para chegar no minimo,
     * mais uma folga (ex: 0.5 = 50% do minimo), para nao ficar no limite logo depois de receber.
     */
    public PedidoCompra criarPedidoDeReposicao(long fornecedorId, double folga) {
        if (folga < 0) {
            throw new RegraDeNegocioException("A folga nao pode ser negativa.");
        }
        List<ItemPedido> itens = produtos.listarTodos().stream()
                .filter(Produto::precisaRepor)
                .map(p -> new ItemPedido(p.getCodigo(), quantidadeSugerida(p, folga)))
                .toList();
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum produto precisa de reposicao agora.");
        }
        return criarPedido(fornecedorId, itens);
    }

    static int quantidadeSugerida(Produto produto, double folga) {
        int extra = (int) Math.ceil(produto.getEstoqueMinimo() * folga);
        return Math.max(1, produto.quantidadeParaRepor() + extra);
    }

    /** Da entrada de tudo no estoque de uma vez. Se algo falhar, nada e gravado. */
    public PedidoCompra receber(long pedidoId) {
        PedidoCompra pedido = buscarPedido(pedidoId);
        LocalDateTime agora = agora();
        pedido.marcarRecebido(agora);

        return transacao.executar(() -> {
            for (ItemCompra item : pedido.getItens()) {
                Produto produto = produtos.buscarPorCodigo(item.codigo())
                        .orElseThrow(() -> new RegraDeNegocioException("Produto " + item.codigo() + " nao encontrado."));
                produto.adicionarEstoque(item.quantidade());
                produtos.salvar(produto);
                movimentacoes.registrar(Movimentacao.entrada(produto.getId(), item.quantidade(), agora,
                        "Pedido de compra #" + pedido.getId() + " - " + pedido.getFornecedorNome()));
            }
            pedidos.atualizarStatus(pedido);
            return pedido;
        });
    }

    public PedidoCompra cancelar(long pedidoId) {
        PedidoCompra pedido = buscarPedido(pedidoId);
        pedido.cancelar();
        pedidos.atualizarStatus(pedido);
        return pedido;
    }

    public PedidoCompra buscarPedido(long id) {
        return pedidos.buscarPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido de compra #" + id + " nao encontrado."));
    }

    public List<PedidoCompra> pedidosEmAberto() {
        return pedidos.listarPorStatus(StatusPedido.ABERTO);
    }

    public List<PedidoCompra> pedidosAtrasados() {
        LocalDate hoje = LocalDate.now(relogio);
        return pedidosEmAberto().stream()
                .filter(p -> p.atrasado(hoje))
                .toList();
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
