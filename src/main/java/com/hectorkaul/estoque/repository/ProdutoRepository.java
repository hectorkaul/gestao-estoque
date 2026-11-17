package com.hectorkaul.estoque.repository;

import com.hectorkaul.estoque.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository {

    /** Insere se o produto ainda nao tem id, senao atualiza. */
    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(long id);

    Optional<Produto> buscarPorCodigo(String codigo);

    List<Produto> listarTodos();

    List<Produto> buscarPorNome(String trecho);
}
