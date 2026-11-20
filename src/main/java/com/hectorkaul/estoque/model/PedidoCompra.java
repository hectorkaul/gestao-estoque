package com.hectorkaul.estoque.model;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PedidoCompra {

    private Long id;
    private final long fornecedorId;
    private final String fornecedorNome;
    private final LocalDateTime dataPedido;
    private final LocalDate previsaoEntrega;
    private StatusPedido status;
    private LocalDateTime dataRecebimento;
    private final List<ItemCompra> itens = new ArrayList<>();

    public PedidoCompra(Long id, long fornecedorId, String fornecedorNome, LocalDateTime dataPedido,
                        LocalDate previsaoEntrega, StatusPedido status, LocalDateTime dataRecebimento) {
        this.id = id;
        this.fornecedorId = fornecedorId;
        this.fornecedorNome = fornecedorNome;
        this.dataPedido = dataPedido;
        this.previsaoEntrega = previsaoEntrega;
        this.status = status;
        this.dataRecebimento = dataRecebimento;
    }

    public static PedidoCompra novo(Fornecedor fornecedor, LocalDateTime agora) {
        LocalDate previsao = agora.toLocalDate().plusDays(fornecedor.getPrazoEntregaDias());
        return new PedidoCompra(null, fornecedor.getId(), fornecedor.getNome(), agora, previsao, StatusPedido.ABERTO, null);
    }

    public void adicionarItem(ItemCompra item) {
        itens.add(item);
    }

    public void marcarRecebido(LocalDateTime quando) {
        exigirAberto("receber");
        status = StatusPedido.RECEBIDO;
        dataRecebimento = quando;
    }

    public void cancelar() {
        exigirAberto("cancelar");
        status = StatusPedido.CANCELADO;
    }

    private void exigirAberto(String acao) {
        if (status != StatusPedido.ABERTO) {
            throw new RegraDeNegocioException("Nao da para " + acao + " o pedido #" + id + ", ele esta " + status + ".");
        }
    }

    /** Pedido aberto que ja passou da data prevista. */
    public boolean atrasado(LocalDate hoje) {
        return status == StatusPedido.ABERTO && hoje.isAfter(previsaoEntrega);
    }

    public BigDecimal total() {
        return itens.stream()
                .map(ItemCompra::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getFornecedorId() {
        return fornecedorId;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public LocalDate getPrevisaoEntrega() {
        return previsaoEntrega;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public LocalDateTime getDataRecebimento() {
        return dataRecebimento;
    }

    public List<ItemCompra> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
