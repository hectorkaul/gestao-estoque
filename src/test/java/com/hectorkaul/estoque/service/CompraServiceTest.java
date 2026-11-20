package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.Fornecedor;
import com.hectorkaul.estoque.model.ItemCompra;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.PedidoCompra;
import com.hectorkaul.estoque.model.StatusPedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompraServiceTest {

    private final BancoDeTeste banco = new BancoDeTeste();

    @AfterEach
    void fechar() {
        banco.close();
    }

    private Fornecedor fornecedor(int prazo) {
        return banco.compras.cadastrarFornecedor(new Fornecedor("Distribuidora Teste", "(53) 0000-0000", prazo));
    }

    @Test
    void pedidoNaoMexeNoEstoqueAteSerRecebido() {
        banco.cadastrar("A1", "10.00", 5, 0);
        Fornecedor f = fornecedor(3);

        PedidoCompra pedido = banco.compras.criarPedido(f.getId(), List.of(new ItemPedido("A1", 20)));

        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
        assertEquals(5, banco.estoque.buscar("A1").getEstoqueAtual());
        assertEquals(BancoDeTeste.AGORA.toLocalDate().plusDays(3), pedido.getPrevisaoEntrega());
    }

    @Test
    void receberDaEntradaEmTodosOsItensEGuardaOHistorico() {
        banco.cadastrar("A1", "10.00", 5, 0);
        banco.cadastrar("B2", "20.00", 0, 0);
        Fornecedor f = fornecedor(2);
        PedidoCompra pedido = banco.compras.criarPedido(f.getId(),
                List.of(new ItemPedido("A1", 10), new ItemPedido("b2", 4)));

        banco.compras.receber(pedido.getId());

        assertEquals(15, banco.estoque.buscar("A1").getEstoqueAtual());
        assertEquals(4, banco.estoque.buscar("B2").getEstoqueAtual());

        PedidoCompra gravado = banco.compras.buscarPedido(pedido.getId());
        assertEquals(StatusPedido.RECEBIDO, gravado.getStatus());
        assertEquals(BancoDeTeste.AGORA, gravado.getDataRecebimento());

        List<Movimentacao> historico = banco.estoque.historico("B2");
        assertEquals("Pedido de compra #" + pedido.getId() + " - Distribuidora Teste", historico.get(0).observacao());
    }

    @Test
    void naoDaParaReceberDuasVezes() {
        banco.cadastrar("A1", "10.00", 0, 0);
        PedidoCompra pedido = banco.compras.criarPedido(fornecedor(1).getId(), List.of(new ItemPedido("A1", 3)));
        banco.compras.receber(pedido.getId());

        assertThrows(RegraDeNegocioException.class, () -> banco.compras.receber(pedido.getId()));
        assertEquals(3, banco.estoque.buscar("A1").getEstoqueAtual());
    }

    @Test
    void pedidoCanceladoNaoPodeSerRecebido() {
        banco.cadastrar("A1", "10.00", 0, 0);
        PedidoCompra pedido = banco.compras.criarPedido(fornecedor(1).getId(), List.of(new ItemPedido("A1", 3)));

        banco.compras.cancelar(pedido.getId());

        assertThrows(RegraDeNegocioException.class, () -> banco.compras.receber(pedido.getId()));
        assertTrue(banco.compras.pedidosEmAberto().isEmpty());
    }

    @Test
    void produtoRepetidoNoPedidoTemAsQuantidadesSomadas() {
        banco.cadastrar("A1", "10.00", 0, 0);

        PedidoCompra pedido = banco.compras.criarPedido(fornecedor(1).getId(),
                List.of(new ItemPedido("A1", 2), new ItemPedido("a1", 5)));

        assertEquals(1, pedido.getItens().size());
        assertEquals(7, pedido.getItens().get(0).quantidade());
    }

    @Test
    void pedidoDeReposicaoCompraOQueFaltaMaisAFolga() {
        banco.cadastrar("A1", "10.00", 8, 10);   // faltam 2, folga 50% de 10 = 5 -> 7
        banco.cadastrar("B2", "10.00", 50, 5);   // ok, nao entra
        banco.cadastrar("C3", "10.00", 4, 4);    // no limite: faltam 0, folga 2 -> 2

        PedidoCompra pedido = banco.compras.criarPedidoDeReposicao(fornecedor(5).getId(), 0.5);

        List<String> itens = pedido.getItens().stream()
                .map(i -> i.codigo() + ":" + i.quantidade())
                .toList();
        assertEquals(List.of("A1:7", "C3:2"), itens);
    }

    @Test
    void pedidoUsaOCustoAtualDoProduto() {
        banco.cadastrar("A1", "10.00", 0, 0); // custo 7,00

        PedidoCompra pedido = banco.compras.criarPedido(fornecedor(1).getId(), List.of(new ItemPedido("A1", 10)));

        ItemCompra item = pedido.getItens().get(0);
        assertEquals(0, new BigDecimal("7.00").compareTo(item.custoUnitario()));
        assertEquals(0, new BigDecimal("70.00").compareTo(pedido.total()));
    }

    @Test
    void pedidoPassaDoPrazoFicaAtrasado() {
        banco.cadastrar("A1", "10.00", 0, 0);
        PedidoCompra pedido = banco.compras.criarPedido(fornecedor(2).getId(), List.of(new ItemPedido("A1", 1)));

        assertTrue(banco.compras.pedidosAtrasados().isEmpty());
        assertTrue(pedido.atrasado(BancoDeTeste.AGORA.toLocalDate().plusDays(3)));
    }
}
