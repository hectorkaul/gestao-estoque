package com.hectorkaul.estoque.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Venda {

    private Long id;
    private final LocalDateTime dataHora;
    private final String cliente;
    private final List<ItemVenda> itens = new ArrayList<>();

    public Venda(Long id, LocalDateTime dataHora, String cliente) {
        this.id = id;
        this.dataHora = dataHora;
        this.cliente = (cliente == null || cliente.isBlank()) ? "Consumidor" : cliente.trim();
    }

    public void adicionarItem(ItemVenda item) {
        itens.add(item);
    }

    public BigDecimal total() {
        return itens.stream()
                .map(ItemVenda::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int quantidadeDeItens() {
        return itens.stream().mapToInt(ItemVenda::quantidade).sum();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getCliente() {
        return cliente;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
