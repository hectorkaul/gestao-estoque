package com.hectorkaul.estoque.repository.jdbc;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.model.ItemCompra;
import com.hectorkaul.estoque.model.PedidoCompra;
import com.hectorkaul.estoque.model.StatusPedido;
import com.hectorkaul.estoque.repository.PedidoCompraRepository;

import java.math.BigDecimal;
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
import java.util.Optional;

public class PedidoCompraRepositoryJdbc implements PedidoCompraRepository {

    private final Database database;

    public PedidoCompraRepositoryJdbc(Database database) {
        this.database = database;
    }

    @Override
    public PedidoCompra salvar(PedidoCompra pedido) {
        String sqlPedido = """
                INSERT INTO pedido_compra (fornecedor_id, data_pedido, previsao_entrega, status)
                VALUES (?, ?, ?, ?)
                """;
        String sqlItem = """
                INSERT INTO item_pedido_compra (pedido_id, produto_id, quantidade, custo_unitario)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = database.getConexao().prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, pedido.getFornecedorId());
            ps.setString(2, pedido.getDataPedido().toString());
            ps.setString(3, pedido.getPrevisaoEntrega().toString());
            ps.setString(4, pedido.getStatus().name());
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    pedido.setId(chaves.getLong(1));
                }
            }

            try (PreparedStatement psItem = database.getConexao().prepareStatement(sqlItem)) {
                for (ItemCompra item : pedido.getItens()) {
                    psItem.setLong(1, pedido.getId());
                    psItem.setLong(2, item.produtoId());
                    psItem.setInt(3, item.quantidade());
                    psItem.setString(4, item.custoUnitario().toPlainString());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }
            return pedido;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar pedido de compra: " + e.getMessage(), e);
        }
    }

    @Override
    public void atualizarStatus(PedidoCompra pedido) {
        String sql = "UPDATE pedido_compra SET status = ?, data_recebimento = ? WHERE id = ?";
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setString(1, pedido.getStatus().name());
            ps.setString(2, pedido.getDataRecebimento() == null ? null : pedido.getDataRecebimento().toString());
            ps.setLong(3, pedido.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar pedido de compra: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<PedidoCompra> buscarPorId(long id) {
        return buscar("WHERE pc.id = ?", id).stream().findFirst();
    }

    @Override
    public List<PedidoCompra> listarPorStatus(StatusPedido status) {
        return buscar("WHERE pc.status = ?", status.name());
    }

    private List<PedidoCompra> buscar(String filtro, Object parametro) {
        String sql = """
                SELECT pc.id AS pedido_id, pc.fornecedor_id, f.nome AS fornecedor, pc.data_pedido,
                       pc.previsao_entrega, pc.status, pc.data_recebimento,
                       i.produto_id, p.codigo, p.nome, i.quantidade, i.custo_unitario
                  FROM pedido_compra pc
                  JOIN fornecedor f ON f.id = pc.fornecedor_id
                  JOIN item_pedido_compra i ON i.pedido_id = pc.id
                  JOIN produto p ON p.id = i.produto_id
                """ + filtro + " ORDER BY pc.previsao_entrega, pc.id, i.id";

        Map<Long, PedidoCompra> pedidos = new LinkedHashMap<>();
        try (PreparedStatement ps = database.getConexao().prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("pedido_id");
                    PedidoCompra pedido = pedidos.get(id);
                    if (pedido == null) {
                        String recebimento = rs.getString("data_recebimento");
                        pedido = new PedidoCompra(id,
                                rs.getLong("fornecedor_id"),
                                rs.getString("fornecedor"),
                                LocalDateTime.parse(rs.getString("data_pedido")),
                                LocalDate.parse(rs.getString("previsao_entrega")),
                                StatusPedido.valueOf(rs.getString("status")),
                                recebimento == null ? null : LocalDateTime.parse(recebimento));
                        pedidos.put(id, pedido);
                    }
                    pedido.adicionarItem(new ItemCompra(
                            rs.getLong("produto_id"),
                            rs.getString("codigo"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            new BigDecimal(rs.getString("custo_unitario"))));
                }
            }
            return new ArrayList<>(pedidos.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar pedidos de compra: " + e.getMessage(), e);
        }
    }
}
