package com.hectorkaul.estoque.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class TransacaoJdbc implements Transacao {

    private final Database database;

    public TransacaoJdbc(Database database) {
        this.database = database;
    }

    @Override
    public <T> T executar(Supplier<T> bloco) {
        Connection conexao = database.getConexao();
        try {
            conexao.setAutoCommit(false);
            T resultado = bloco.get();
            conexao.commit();
            return resultado;
        } catch (RuntimeException e) {
            desfazer(conexao);
            throw e;
        } catch (SQLException e) {
            desfazer(conexao);
            throw new IllegalStateException("Erro na transacao: " + e.getMessage(), e);
        } finally {
            try {
                conexao.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // a conexao ja deve estar com problema, o erro original e mais importante
            }
        }
    }

    private void desfazer(Connection conexao) {
        try {
            conexao.rollback();
        } catch (SQLException e) {
            System.err.println("Falha no rollback: " + e.getMessage());
        }
    }
}
