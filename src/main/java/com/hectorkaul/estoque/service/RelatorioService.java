package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.model.ProdutoVendido;
import com.hectorkaul.estoque.model.Venda;
import com.hectorkaul.estoque.repository.VendaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RelatorioService {

    private final VendaRepository vendas;

    public RelatorioService(VendaRepository vendas) {
        this.vendas = vendas;
    }

    public record ResumoVendas(int quantidadeVendas, BigDecimal faturamento, BigDecimal ticketMedio) {
    }

    public ResumoVendas resumo(LocalDate inicio, LocalDate fim) {
        List<Venda> doPeriodo = vendas.listarPorPeriodo(inicio, fim);
        BigDecimal total = doPeriodo.stream()
                .map(Venda::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = doPeriodo.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(doPeriodo.size()), 2, RoundingMode.HALF_UP);
        return new ResumoVendas(doPeriodo.size(), total, ticket);
    }

    /** Faturamento agrupado por mes, em ordem cronologica. */
    public Map<YearMonth, BigDecimal> faturamentoPorMes() {
        return vendas.listarTodas().stream()
                .collect(Collectors.groupingBy(
                        venda -> YearMonth.from(venda.getDataHora()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Venda::total, BigDecimal::add)));
    }

    /** Quanto cada cliente comprou, do maior para o menor. */
    public Map<String, BigDecimal> totalPorCliente() {
        Map<String, BigDecimal> porCliente = vendas.listarTodas().stream()
                .collect(Collectors.groupingBy(
                        Venda::getCliente,
                        Collectors.reducing(BigDecimal.ZERO, Venda::total, BigDecimal::add)));

        return porCliente.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    public List<ProdutoVendido> maisVendidos(int limite) {
        return vendas.maisVendidos(limite);
    }
}
