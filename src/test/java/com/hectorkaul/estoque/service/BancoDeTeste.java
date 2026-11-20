package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.db.TransacaoJdbc;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.repository.jdbc.FornecedorRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.MovimentacaoRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.PedidoCompraRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.ProdutoRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.VendaRepositoryJdbc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Monta os services com um SQLite em memoria, novo a cada teste. */
class BancoDeTeste implements AutoCloseable {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 17, 10, 0);

    final Database database = Database.emMemoria();
    final ProdutoRepositoryJdbc produtos = new ProdutoRepositoryJdbc(database);
    final MovimentacaoRepositoryJdbc movimentacoes = new MovimentacaoRepositoryJdbc(database);
    final VendaRepositoryJdbc vendas = new VendaRepositoryJdbc(database);
    final Clock relogio = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    final EstoqueService estoque = new EstoqueService(produtos, movimentacoes, relogio);
    final VendaService vendaService = new VendaService(produtos, movimentacoes, vendas, new TransacaoJdbc(database), relogio);
    final RelatorioService relatorios = new RelatorioService(vendas);
    final CompraService compras = new CompraService(new FornecedorRepositoryJdbc(database),
            new PedidoCompraRepositoryJdbc(database), produtos, movimentacoes, new TransacaoJdbc(database), relogio);

    BancoDeTeste() {
        database.criarTabelas();
    }

    Produto cadastrar(String codigo, String preco, int estoqueInicial, int minimo) {
        Produto produto = estoque.cadastrar(new Produto(codigo, "Produto " + codigo, "Teste",
                new BigDecimal(preco).multiply(new BigDecimal("0.7")), new BigDecimal(preco), minimo));
        if (estoqueInicial > 0) {
            estoque.registrarEntrada(codigo, estoqueInicial, "inicial");
        }
        return produto;
    }

    @Override
    public void close() {
        database.close();
    }
}
