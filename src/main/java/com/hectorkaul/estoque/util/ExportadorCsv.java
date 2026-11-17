package com.hectorkaul.estoque.util;

import com.hectorkaul.estoque.model.ItemVenda;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.model.Venda;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera CSV no padrao brasileiro (separador ; e virgula decimal),
 * que abre direto no Excel e importa sem ajuste no Power BI.
 */
public class ExportadorCsv {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportarProdutos(List<Produto> produtos, Path arquivo) {
        escrever(arquivo, out -> {
            out.write("codigo;nome;categoria;preco_custo;preco_venda;estoque_atual;estoque_minimo;valor_em_estoque");
            out.newLine();
            for (Produto p : produtos) {
                out.write(String.join(";",
                        p.getCodigo(),
                        texto(p.getNome()),
                        texto(p.getCategoria()),
                        decimal(p.getPrecoCusto()),
                        decimal(p.getPrecoVenda()),
                        String.valueOf(p.getEstoqueAtual()),
                        String.valueOf(p.getEstoqueMinimo()),
                        decimal(p.valorEmEstoque())));
                out.newLine();
            }
        });
    }

    /** Uma linha por item vendido, que e o formato mais facil de usar em tabela dinamica. */
    public void exportarVendas(List<Venda> vendas, Path arquivo) {
        escrever(arquivo, out -> {
            out.write("venda;data;cliente;produto;quantidade;preco_unitario;subtotal");
            out.newLine();
            for (Venda venda : vendas) {
                for (ItemVenda item : venda.getItens()) {
                    out.write(String.join(";",
                            String.valueOf(venda.getId()),
                            venda.getDataHora().format(DATA_HORA),
                            texto(venda.getCliente()),
                            texto(item.descricao()),
                            String.valueOf(item.quantidade()),
                            decimal(item.precoUnitario()),
                            decimal(item.subtotal())));
                    out.newLine();
                }
            }
        });
    }

    private interface Escrita {
        void em(BufferedWriter out) throws IOException;
    }

    private void escrever(Path arquivo, Escrita escrita) {
        try {
            if (arquivo.getParent() != null) {
                Files.createDirectories(arquivo.getParent());
            }
            try (BufferedWriter out = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
                out.write('﻿'); // BOM, senao o Excel abre os acentos errado
                escrita.em(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gravar " + arquivo, e);
        }
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
