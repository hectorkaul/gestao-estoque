package com.hectorkaul.estoque.repository.jdbc;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.model.Fornecedor;
import com.hectorkaul.estoque.repository.FornecedorRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FornecedorRepositoryJdbc implements FornecedorRepository {

    private final Database database;

    public FornecedorRepositoryJdbc(Database database) {
        this.database = database;
    }

    @Override
    public Fornecedor salvar(Fornecedor fornecedor) {
        String sql = "INSERT INTO fornecedor (nome, contato, prazo_entrega_dias) VALUES (?, ?, ?)";
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fornecedor.getNome());
            ps.setString(2, fornecedor.getContato());
            ps.setInt(3, fornecedor.getPrazoEntregaDias());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    fornecedor.setId(chaves.getLong(1));
                }
            }
            return fornecedor;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar fornecedor: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Fornecedor> buscarPorId(long id) {
        String sql = "SELECT id, nome, contato, prazo_entrega_dias FROM fornecedor WHERE id = ?";
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(montar(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar fornecedor: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Fornecedor> listarTodos() {
        String sql = "SELECT id, nome, contato, prazo_entrega_dias FROM fornecedor ORDER BY nome";
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Fornecedor> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(montar(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar fornecedores: " + e.getMessage(), e);
        }
    }

    private Fornecedor montar(ResultSet rs) throws SQLException {
        return new Fornecedor(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("contato"),
                rs.getInt("prazo_entrega_dias"));
    }
}
