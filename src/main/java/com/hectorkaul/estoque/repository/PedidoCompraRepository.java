package com.hectorkaul.estoque.repository;

import com.hectorkaul.estoque.model.PedidoCompra;
import com.hectorkaul.estoque.model.StatusPedido;

import java.util.List;
import java.util.Optional;

public interface PedidoCompraRepository {

    /** Grava um pedido novo com os itens. */
    PedidoCompra salvar(PedidoCompra pedido);

    /** Atualiza so o status e a data de recebimento (os itens nao mudam depois de criados). */
    void atualizarStatus(PedidoCompra pedido);

    Optional<PedidoCompra> buscarPorId(long id);

    List<PedidoCompra> listarPorStatus(StatusPedido status);
}
