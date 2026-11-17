package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.Venda;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaServiceTest {

    private final BancoDeTeste banco = new BancoDeTeste();

    @AfterEach
    void fechar() {
        banco.close();
    }

    @Test
    void vendaBaixaOEstoqueECalculaOTotal() {
        banco.cadastrar("A1", "10.00", 10, 0);
        banco.cadastrar("B2", "2.50", 100, 0);

        Venda venda = banco.vendaService.registrarVenda("Maria",
                List.of(new ItemPedido("A1", 3), new ItemPedido("B2", 4)));

        assertNotNull(venda.getId());
        assertEquals(0, new BigDecimal("40.00").compareTo(venda.total()));
        assertEquals(7, banco.estoque.buscar("A1").getEstoqueAtual());
        assertEquals(96, banco.estoque.buscar("B2").getEstoqueAtual());
    }

    @Test
    void vendaFicaGravadaComOsItens() {
        banco.cadastrar("A1", "10.00", 10, 0);
        banco.vendaService.registrarVenda("", List.of(new ItemPedido("A1", 2)));

        List<Venda> gravadas = banco.vendaService.listar();
        assertEquals(1, gravadas.size());
        assertEquals("Consumidor", gravadas.get(0).getCliente());
        assertEquals(1, gravadas.get(0).getItens().size());
        assertEquals("Produto A1", gravadas.get(0).getItens().get(0).descricao());
    }

    @Test
    void vendaGeraMovimentacaoDeSaida() {
        banco.cadastrar("A1", "10.00", 10, 0);
        Venda venda = banco.vendaService.registrarVenda("Joao", List.of(new ItemPedido("A1", 2)));

        var historico = banco.estoque.historico("A1");
        assertEquals(2, historico.size()); // entrada inicial + venda
        assertEquals("Venda #" + venda.getId(), historico.get(1).observacao());
    }

    @Test
    void seUmItemNaoTemEstoqueAVendaInteiraERecusada() {
        banco.cadastrar("A1", "10.00", 10, 0);
        banco.cadastrar("B2", "10.00", 1, 0);

        assertThrows(RegraDeNegocioException.class, () -> banco.vendaService.registrarVenda("Joao",
                List.of(new ItemPedido("A1", 5), new ItemPedido("B2", 2))));

        // nada pode ter mudado
        assertEquals(10, banco.estoque.buscar("A1").getEstoqueAtual());
        assertEquals(1, banco.estoque.buscar("B2").getEstoqueAtual());
        assertTrue(banco.vendaService.listar().isEmpty());
    }

    @Test
    void mesmoProdutoDigitadoDuasVezesSomaAsQuantidades() {
        banco.cadastrar("A1", "10.00", 5, 0);

        // 3 + 3 = 6, mais do que os 5 em estoque
        assertThrows(RegraDeNegocioException.class, () -> banco.vendaService.registrarVenda("Joao",
                List.of(new ItemPedido("A1", 3), new ItemPedido("a1", 3))));

        Venda venda = banco.vendaService.registrarVenda("Joao",
                List.of(new ItemPedido("A1", 2), new ItemPedido("a1", 3)));
        assertEquals(1, venda.getItens().size());
        assertEquals(5, venda.getItens().get(0).quantidade());
    }

    @Test
    void vendaSemItensNaoEPermitida() {
        assertThrows(RegraDeNegocioException.class, () -> banco.vendaService.registrarVenda("Joao", List.of()));
    }
}
