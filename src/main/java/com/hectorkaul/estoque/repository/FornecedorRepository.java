package com.hectorkaul.estoque.repository;

import com.hectorkaul.estoque.model.Fornecedor;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepository {

    Fornecedor salvar(Fornecedor fornecedor);

    Optional<Fornecedor> buscarPorId(long id);

    List<Fornecedor> listarTodos();
}
