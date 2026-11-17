package com.hectorkaul.estoque.repository.jdbc;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.TipoMovimentacao;
import com.hectorkaul.estoque.repository.MovimentacaoRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoRepositoryJdbc implements MovimentacaoRepository {

    private final Database database;

    public MovimentacaoRepositoryJdbc(Database database) {
        this.database = database;
    }

    @Override
    public void registrar(Movimentacao m) {
        String sql = """
                INSERT INTO movimentacao (produto_id, tipo, quantidade, data_hora, observacao)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setLong(1, m.produtoId());
            ps.setString(2, m.tipo().name());
            ps.setInt(3, m.quantidade());
            ps.setString(4, m.dataHora().toString());
            ps.setString(5, m.observacao());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao registrar movimentacao: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Movimentacao> listarPorProduto(long produtoId) {
        String sql = """
                SELECT id, produto_id, tipo, quantidade, data_hora, observacao
                  FROM movimentacao
                 WHERE produto_id = ?
                 ORDER BY data_hora, id
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setLong(1, produtoId);
            List<Movimentacao> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Movimentacao(
                            rs.getLong("id"),
                            rs.getLong("produto_id"),
                            TipoMovimentacao.valueOf(rs.getString("tipo")),
                            rs.getInt("quantidade"),
                            LocalDateTime.parse(rs.getString("data_hora")),
                            rs.getString("observacao")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar movimentacoes: " + e.getMessage(), e);
        }
    }
}
