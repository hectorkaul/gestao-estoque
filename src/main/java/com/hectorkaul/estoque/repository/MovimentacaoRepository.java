package com.hectorkaul.estoque.repository;

import com.hectorkaul.estoque.model.Movimentacao;

import java.util.List;

public interface MovimentacaoRepository {

    void registrar(Movimentacao movimentacao);

    List<Movimentacao> listarPorProduto(long produtoId);
}
