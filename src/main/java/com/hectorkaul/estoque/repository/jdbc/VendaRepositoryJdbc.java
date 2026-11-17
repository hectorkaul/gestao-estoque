package com.hectorkaul.estoque.repository.jdbc;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.model.ItemVenda;
import com.hectorkaul.estoque.model.ProdutoVendido;
import com.hectorkaul.estoque.model.Venda;
import com.hectorkaul.estoque.repository.VendaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendaRepositoryJdbc implements VendaRepository {

    private final Database database;

    public VendaRepositoryJdbc(Database database) {
        this.database = database;
    }

    /**
     * Grava a venda e os itens. Quem chama (VendaService) ja abre a transacao,
     * entao aqui nao tem commit.
     */
    @Override
    public Venda salvar(Venda venda) {
        String sqlVenda = "INSERT INTO venda (data_hora, cliente) VALUES (?, ?)";
        String sqlItem = """
                INSERT INTO item_venda (venda_id, produto_id, quantidade, preco_unitario)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement psVenda = database.getConexao().prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
            psVenda.setString(1, venda.getDataHora().toString());
            psVenda.setString(2, venda.getCliente());
            psVenda.executeUpdate();
            try (ResultSet chaves = psVenda.getGeneratedKeys()) {
                if (chaves.next()) {
                    venda.setId(chaves.getLong(1));
                }
            }

            try (PreparedStatement psItem = database.getConexao().prepareStatement(sqlItem)) {
                for (ItemVenda item : venda.getItens()) {
                    psItem.setLong(1, venda.getId());
                    psItem.setLong(2, item.produtoId());
                    psItem.setInt(3, item.quantidade());
                    psItem.setString(4, item.precoUnitario().toPlainString());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }
            return venda;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar venda: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Venda> listarTodas() {
        return buscarVendas("", List.of());
    }

    @Override
    public List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        // data_hora fica gravada como texto ISO (2026-11-17T10:30), entao comparar texto funciona
        String filtro = "WHERE v.data_hora >= ? AND v.data_hora < ?";
        return buscarVendas(filtro, List.of(inicio.atStartOfDay().toString(), fim.plusDays(1).atStartOfDay().toString()));
    }

    private List<Venda> buscarVendas(String filtro, List<Object> parametros) {
        String sql = """
                SELECT v.id AS venda_id, v.data_hora, v.cliente,
                       i.produto_id, p.nome, i.quantidade, i.preco_unitario
                  FROM venda v
                  JOIN item_venda i ON i.venda_id = v.id
                  JOIN produto p ON p.id = i.produto_id
                """ + filtro + " ORDER BY v.data_hora, v.id, i.id";

        // varias linhas por venda (uma por item), entao junto pelo id
        Map<Long, Venda> vendas = new LinkedHashMap<>();
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long vendaId = rs.getLong("venda_id");
                    Venda venda = vendas.get(vendaId);
                    if (venda == null) {
                        venda = new Venda(vendaId, LocalDateTime.parse(rs.getString("data_hora")), rs.getString("cliente"));
                        vendas.put(vendaId, venda);
                    }
                    venda.adicionarItem(new ItemVenda(
                            rs.getLong("produto_id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            new BigDecimal(rs.getString("preco_unitario"))));
                }
            }
            return new ArrayList<>(vendas.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar vendas: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProdutoVendido> maisVendidos(int limite) {
        String sql = """
                SELECT p.codigo,
                       p.nome,
                       SUM(i.quantidade) AS quantidade,
                       SUM(i.quantidade * CAST(i.preco_unitario AS REAL)) AS faturamento
                  FROM item_venda i
                  JOIN produto p ON p.id = i.produto_id
                 GROUP BY p.id, p.codigo, p.nome
                 ORDER BY quantidade DESC, faturamento DESC
                 LIMIT ?
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setInt(1, limite);
            List<ProdutoVendido> ranking = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ranking.add(new ProdutoVendido(
                            rs.getString("codigo"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            BigDecimal.valueOf(rs.getDouble("faturamento")).setScale(2, RoundingMode.HALF_UP)));
                }
            }
            return ranking;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no ranking de produtos: " + e.getMessage(), e);
        }
    }
}
