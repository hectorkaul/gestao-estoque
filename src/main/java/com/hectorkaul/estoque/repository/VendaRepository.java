package com.hectorkaul.estoque.repository;

import com.hectorkaul.estoque.model.ProdutoVendido;
import com.hectorkaul.estoque.model.Venda;

import java.time.LocalDate;
import java.util.List;

public interface VendaRepository {

    Venda salvar(Venda venda);

    List<Venda> listarTodas();

    List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim);

    /** Ranking feito direto no SQL, agrupando os itens vendidos por produto. */
    List<ProdutoVendido> maisVendidos(int limite);
}
