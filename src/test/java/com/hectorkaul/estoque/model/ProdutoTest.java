package com.hectorkaul.estoque.model;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoTest {

    private Produto cimento() {
        return new Produto("cim50", "Cimento 50kg", "Basico", new BigDecimal("30.00"), new BigDecimal("45.00"), 10);
    }

    @Test
    void deveDeixarCodigoEmMaiusculo() {
        assertEquals("CIM50", cimento().getCodigo());
    }

    @Test
    void deveCalcularMargemSobreOCusto() {
        assertEquals(new BigDecimal("50.0"), cimento().margemPercentual());
    }

    @Test
    void naoDevePermitirRemoverMaisDoQueTemNoEstoque() {
        Produto produto = cimento();
        produto.adicionarEstoque(5);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> produto.removerEstoque(6));
        assertTrue(erro.getMessage().contains("Estoque insuficiente"));
        assertEquals(5, produto.getEstoqueAtual());
    }

    @Test
    void precisaReporQuandoChegaNoMinimo() {
        Produto produto = cimento();
        produto.adicionarEstoque(10);
        assertTrue(produto.precisaRepor());

        produto.adicionarEstoque(1);
        assertFalse(produto.precisaRepor());
    }

    @Test
    void naoDeveAceitarPrecoDeVendaZerado() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Produto("X1", "Teste", "Geral", BigDecimal.ONE, BigDecimal.ZERO, 0));
    }

    @Test
    void valorEmEstoqueUsaOPrecoDeCusto() {
        Produto produto = cimento();
        produto.adicionarEstoque(4);
        assertEquals(new BigDecimal("120.00"), produto.valorEmEstoque());
    }
}
