package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.model.ProdutoVendido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatorioServiceTest {

    private final BancoDeTeste banco = new BancoDeTeste();

    @BeforeEach
    void prepararVendas() {
        banco.cadastrar("A1", "10.00", 100, 0);
        banco.cadastrar("B2", "50.00", 100, 0);

        vender("Maria", LocalDateTime.of(2026, 10, 5, 9, 0), "A1", 2);   // 20
        vender("Joao", LocalDateTime.of(2026, 10, 20, 15, 0), "B2", 1);  // 50
        vender("Maria", LocalDateTime.of(2026, 11, 3, 11, 0), "A1", 5);  // 50
        vender("Maria", LocalDateTime.of(2026, 11, 10, 16, 0), "B2", 2); // 100
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    private void vender(String cliente, LocalDateTime quando, String codigo, int quantidade) {
        banco.vendaService.registrarVenda(cliente, List.of(new ItemPedido(codigo, quantidade)), quando);
    }

    @Test
    void faturamentoPorMesAgrupaEOrdena() {
        Map<YearMonth, BigDecimal> porMes = banco.relatorios.faturamentoPorMes();

        assertEquals(List.of(YearMonth.of(2026, 10), YearMonth.of(2026, 11)), List.copyOf(porMes.keySet()));
        assertEquals(0, new BigDecimal("70").compareTo(porMes.get(YearMonth.of(2026, 10))));
        assertEquals(0, new BigDecimal("150").compareTo(porMes.get(YearMonth.of(2026, 11))));
    }

    @Test
    void resumoConsideraSoOPeriodoInformado() {
        RelatorioService.ResumoVendas resumo = banco.relatorios.resumo(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertEquals(2, resumo.quantidadeVendas());
        assertEquals(0, new BigDecimal("150").compareTo(resumo.faturamento()));
        assertEquals(new BigDecimal("75.00"), resumo.ticketMedio());
    }

    @Test
    void periodoSemVendasRetornaZero() {
        RelatorioService.ResumoVendas resumo = banco.relatorios.resumo(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertEquals(0, resumo.quantidadeVendas());
        assertEquals(BigDecimal.ZERO, resumo.ticketMedio());
    }

    @Test
    void rankingPorQuantidadeVendida() {
        List<ProdutoVendido> ranking = banco.relatorios.maisVendidos(10);

        assertEquals("A1", ranking.get(0).codigo());
        assertEquals(7, ranking.get(0).quantidade());
        assertEquals(new BigDecimal("70.00"), ranking.get(0).faturamento());
        assertEquals(3, ranking.get(1).quantidade());
    }

    @Test
    void melhorClienteAparecePrimeiro() {
        Map<String, BigDecimal> porCliente = banco.relatorios.totalPorCliente();

        assertEquals("Maria", porCliente.keySet().iterator().next());
        assertEquals(0, new BigDecimal("170").compareTo(porCliente.get("Maria")));
    }
}
