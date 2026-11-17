package com.hectorkaul.estoque;

import com.hectorkaul.estoque.db.Database;
import com.hectorkaul.estoque.db.TransacaoJdbc;
import com.hectorkaul.estoque.repository.MovimentacaoRepository;
import com.hectorkaul.estoque.repository.ProdutoRepository;
import com.hectorkaul.estoque.repository.VendaRepository;
import com.hectorkaul.estoque.repository.jdbc.MovimentacaoRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.ProdutoRepositoryJdbc;
import com.hectorkaul.estoque.repository.jdbc.VendaRepositoryJdbc;
import com.hectorkaul.estoque.service.EstoqueService;
import com.hectorkaul.estoque.service.RelatorioService;
import com.hectorkaul.estoque.service.VendaService;
import com.hectorkaul.estoque.ui.Entrada;
import com.hectorkaul.estoque.ui.Menu;
import com.hectorkaul.estoque.util.ExportadorCsv;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        List<String> argumentos = Arrays.asList(args);
        boolean demo = argumentos.contains("--demo");
        String arquivoBanco = argumentos.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .orElse("estoque.db");

        try (Database database = Database.arquivo(arquivoBanco);
             Scanner scanner = new Scanner(System.in)) {

            database.criarTabelas();

            ProdutoRepository produtos = new ProdutoRepositoryJdbc(database);
            MovimentacaoRepository movimentacoes = new MovimentacaoRepositoryJdbc(database);
            VendaRepository vendas = new VendaRepositoryJdbc(database);
            Clock relogio = Clock.systemDefaultZone();

            EstoqueService estoqueService = new EstoqueService(produtos, movimentacoes, relogio);
            VendaService vendaService = new VendaService(produtos, movimentacoes, vendas,
                    new TransacaoJdbc(database), relogio);
            RelatorioService relatorioService = new RelatorioService(vendas);

            if (demo) {
                if (produtos.listarTodos().isEmpty()) {
                    System.out.println("Gerando dados de exemplo (3 meses de vendas)...");
                    new DadosExemplo(estoqueService, vendaService).carregar(3);
                    System.out.println("Pronto.\n");
                } else {
                    System.out.println("O banco ja tem produtos, nao gerei dados de exemplo.\n");
                }
            }

            new Menu(estoqueService, vendaService, relatorioService, new ExportadorCsv(), new Entrada(scanner))
                    .iniciar();
        }
    }
}
