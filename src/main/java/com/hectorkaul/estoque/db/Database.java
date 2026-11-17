package com.hectorkaul.estoque.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Guarda a conexao com o SQLite. Como e um sistema de console com um usuario so,
 * uma conexao aberta durante toda a execucao e suficiente.
 */
public class Database implements AutoCloseable {

    private final String url;
    private Connection conexao;

    public Database(String url) {
        this.url = url;
    }

    public static Database arquivo(String caminho) {
        return new Database("jdbc:sqlite:" + caminho);
    }

    /** Banco so em memoria, usado nos testes. */
    public static Database emMemoria() {
        return new Database("jdbc:sqlite::memory:");
    }

    public Connection getConexao() {
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = DriverManager.getConnection(url);
                try (Statement st = conexao.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
            }
            return conexao;
        } catch (SQLException e) {
            throw new IllegalStateException("Nao foi possivel conectar no banco: " + e.getMessage(), e);
        }
    }

    public void criarTabelas() {
        String script = lerArquivo("/schema.sql");
        try (Statement st = getConexao().createStatement()) {
            for (String comando : script.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao criar as tabelas: " + e.getMessage(), e);
        }
    }

    private String lerArquivo(String caminho) {
        try (InputStream in = Database.class.getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IllegalStateException("Arquivo nao encontrado: " + caminho);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }
        } catch (SQLException e) {
            // fechando o programa, nao tem muito o que fazer aqui
            System.err.println("Aviso: erro ao fechar o banco: " + e.getMessage());
        }
    }
}
