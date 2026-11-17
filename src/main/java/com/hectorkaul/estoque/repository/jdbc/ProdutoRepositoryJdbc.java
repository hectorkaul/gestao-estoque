package com.hectorkaul.estoque.repository.jdbc;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositoryJdbc implements ProdutoRepository {

    private static final String COLUNAS =
            "id, codigo, nome, categoria, preco_custo, preco_venda, estoque_atual, estoque_minimo";

    private final Database database;

    public ProdutoRepositoryJdbc(Database database) {
        this.database = database;
    }

    @Override
    public Produto salvar(Produto produto) {
        if (produto.getId() == null) {
            inserir(produto);
        } else {
            atualizar(produto);
        }
        return produto;
    }

    private void inserir(Produto p) {
        String sql = """
                INSERT INTO produto (codigo, nome, categoria, preco_custo, preco_venda, estoque_atual, estoque_minimo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getCategoria());
            ps.setString(4, p.getPrecoCusto().toPlainString());
            ps.setString(5, p.getPrecoVenda().toPlainString());
            ps.setInt(6, p.getEstoqueAtual());
            ps.setInt(7, p.getEstoqueMinimo());
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    p.setId(chaves.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao inserir produto: " + e.getMessage(), e);
        }
    }

    private void atualizar(Produto p) {
        String sql = """
                UPDATE produto
                   SET nome = ?, categoria = ?, preco_custo = ?, preco_venda = ?,
                       estoque_atual = ?, estoque_minimo = ?
                 WHERE id = ?
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getCategoria());
            ps.setString(3, p.getPrecoCusto().toPlainString());
            ps.setString(4, p.getPrecoVenda().toPlainString());
            ps.setInt(5, p.getEstoqueAtual());
            ps.setInt(6, p.getEstoqueMinimo());
            ps.setLong(7, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar produto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        List<Produto> encontrados = consultar("SELECT " + COLUNAS + " FROM produto WHERE id = ?", id);
        return encontrados.stream().findFirst();
    }

    @Override
    public Optional<Produto> buscarPorCodigo(String codigo) {
        List<Produto> encontrados = consultar(
                "SELECT " + COLUNAS + " FROM produto WHERE codigo = ?", codigo.trim().toUpperCase());
        return encontrados.stream().findFirst();
    }

    @Override
    public List<Produto> listarTodos() {
        return consultar("SELECT " + COLUNAS + " FROM produto ORDER BY categoria, nome");
    }

    @Override
    public List<Produto> buscarPorNome(String trecho) {
        return consultar("SELECT " + COLUNAS + " FROM produto WHERE nome LIKE ? ORDER BY nome",
                "%" + trecho.trim() + "%");
    }

    private List<Produto> consultar(String sql, Object... parametros) {
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            List<Produto> produtos = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produtos.add(montar(rs));
                }
            }
            return produtos;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao consultar produtos: " + e.getMessage(), e);
        }
    }

    private Produto montar(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("nome"),
                rs.getString("categoria"),
                new BigDecimal(rs.getString("preco_custo")),
                new BigDecimal(rs.getString("preco_venda")),
                rs.getInt("estoque_atual"),
                rs.getInt("estoque_minimo"));
    }
}
