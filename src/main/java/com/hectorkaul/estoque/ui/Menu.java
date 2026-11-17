package com.hectorkaul.estoque.ui;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.ItemVenda;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.model.ProdutoVendido;
import com.hectorkaul.estoque.model.Venda;
import com.hectorkaul.estoque.service.EstoqueService;
import com.hectorkaul.estoque.service.ItemPedido;
import com.hectorkaul.estoque.service.RelatorioService;
import com.hectorkaul.estoque.service.VendaService;
import com.hectorkaul.estoque.util.ExportadorCsv;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Menu {

    private final EstoqueService estoque;
    private final VendaService vendas;
    private final RelatorioService relatorios;
    private final ExportadorCsv exportador;
    private final Entrada entrada;

    public Menu(EstoqueService estoque, VendaService vendas, RelatorioService relatorios,
                ExportadorCsv exportador, Entrada entrada) {
        this.estoque = estoque;
        this.vendas = vendas;
        this.relatorios = relatorios;
        this.exportador = exportador;
        this.entrada = entrada;
    }

    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            mostrarOpcoes();
            String opcao = entrada.texto("Opcao: ");
            System.out.println();
            try {
                continuar = executar(opcao);
            } catch (RegraDeNegocioException e) {
                System.out.println(">> " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println("Ate mais!");
    }

    private void mostrarOpcoes() {
        System.out.println("========== CONTROLE DE ESTOQUE ==========");
        System.out.println(" 1 - Cadastrar produto");
        System.out.println(" 2 - Listar produtos");
        System.out.println(" 3 - Pesquisar produto por nome");
        System.out.println(" 4 - Entrada de mercadoria");
        System.out.println(" 5 - Saida manual (perda, uso interno)");
        System.out.println(" 6 - Registrar venda");
        System.out.println(" 7 - Produtos para repor");
        System.out.println(" 8 - Historico de um produto");
        System.out.println(" 9 - Relatorios de vendas");
        System.out.println("10 - Exportar CSV (Excel / Power BI)");
        System.out.println(" 0 - Sair");
    }

    private boolean executar(String opcao) {
        switch (opcao) {
            case "1" -> cadastrarProduto();
            case "2" -> imprimirProdutos(estoque.listar());
            case "3" -> imprimirProdutos(estoque.pesquisar(entrada.textoObrigatorio("Parte do nome: ")));
            case "4" -> entradaDeMercadoria();
            case "5" -> saidaManual();
            case "6" -> registrarVenda();
            case "7" -> produtosParaRepor();
            case "8" -> historico();
            case "9" -> relatorios();
            case "10" -> exportar();
            case "0" -> {
                return false;
            }
            default -> System.out.println("Opcao invalida.");
        }
        return true;
    }

    private void cadastrarProduto() {
        String codigo = entrada.textoObrigatorio("Codigo: ");
        String nome = entrada.textoObrigatorio("Nome: ");
        String categoria = entrada.texto("Categoria: ");
        BigDecimal custo = entrada.decimal("Preco de custo: ");
        BigDecimal venda = entrada.decimal("Preco de venda: ");
        int minimo = entrada.inteiro("Estoque minimo: ", 0);

        Produto produto = estoque.cadastrar(new Produto(codigo, nome, categoria, custo, venda, minimo));
        System.out.println("Produto cadastrado: " + produto + " (margem de " + produto.margemPercentual() + "%)");

        int inicial = entrada.inteiro("Quantidade inicial em estoque (0 para nenhuma): ", 0);
        if (inicial > 0) {
            estoque.registrarEntrada(produto.getCodigo(), inicial, "Estoque inicial");
        }
    }

    private void imprimirProdutos(List<Produto> lista) {
        if (lista.isEmpty()) {
            System.out.println("Nenhum produto encontrado.");
            return;
        }
        System.out.printf("%-8s %-30s %-14s %12s %7s %7s%n", "CODIGO", "NOME", "CATEGORIA", "PRECO", "ESTOQ.", "MIN.");
        for (Produto p : lista) {
            System.out.printf("%-8s %-30s %-14s %12s %7d %7d%s%n",
                    p.getCodigo(),
                    Formato.cortar(p.getNome(), 30),
                    Formato.cortar(p.getCategoria(), 14),
                    Formato.moeda(p.getPrecoVenda()),
                    p.getEstoqueAtual(),
                    p.getEstoqueMinimo(),
                    p.precisaRepor() ? "  <- repor" : "");
        }
        System.out.println("Valor total em estoque (custo): " + Formato.moeda(estoque.valorTotalEmEstoque()));
    }

    private void entradaDeMercadoria() {
        String codigo = entrada.textoObrigatorio("Codigo do produto: ");
        int quantidade = entrada.inteiro("Quantidade recebida: ", 1);
        String nota = entrada.texto("Nota fiscal / fornecedor (opcional): ");
        Produto produto = estoque.registrarEntrada(codigo, quantidade, nota.isEmpty() ? "Entrada" : nota);
        System.out.println("Estoque de " + produto.getNome() + " agora: " + produto.getEstoqueAtual());
    }

    private void saidaManual() {
        String codigo = entrada.textoObrigatorio("Codigo do produto: ");
        int quantidade = entrada.inteiro("Quantidade: ", 1);
        String motivo = entrada.textoObrigatorio("Motivo: ");
        Produto produto = estoque.registrarSaida(codigo, quantidade, motivo);
        System.out.println("Estoque de " + produto.getNome() + " agora: " + produto.getEstoqueAtual());
    }

    private void registrarVenda() {
        String cliente = entrada.texto("Cliente (enter para consumidor): ");
        List<ItemPedido> itens = new ArrayList<>();

        System.out.println("Digite os itens. Codigo vazio termina.");
        while (true) {
            String codigo = entrada.texto("  Codigo: ");
            if (codigo.isEmpty()) {
                break;
            }
            Produto produto = estoque.buscar(codigo);
            System.out.println("  " + produto.getNome() + " - " + Formato.moeda(produto.getPrecoVenda())
                    + " (estoque: " + produto.getEstoqueAtual() + ")");
            int quantidade = entrada.inteiro("  Quantidade: ", 1);
            itens.add(new ItemPedido(codigo, quantidade));
        }

        if (itens.isEmpty()) {
            System.out.println("Venda cancelada, nenhum item.");
            return;
        }

        Venda venda = vendas.registrarVenda(cliente, itens);
        System.out.println();
        System.out.println("Venda #" + venda.getId() + " - " + venda.getCliente());
        for (ItemVenda item : venda.getItens()) {
            System.out.printf("  %3dx %-30s %12s%n", item.quantidade(), Formato.cortar(item.descricao(), 30),
                    Formato.moeda(item.subtotal()));
        }
        System.out.println("  TOTAL: " + Formato.moeda(venda.total()));
    }

    private void produtosParaRepor() {
        List<Produto> repor = estoque.produtosParaRepor();
        if (repor.isEmpty()) {
            System.out.println("Nenhum produto abaixo do minimo.");
            return;
        }
        System.out.println("Produtos no minimo ou abaixo:");
        repor.forEach(p -> System.out.printf("  %-8s %-30s estoque %4d | minimo %4d | comprar pelo menos %d%n",
                p.getCodigo(), Formato.cortar(p.getNome(), 30), p.getEstoqueAtual(), p.getEstoqueMinimo(),
                Math.max(p.quantidadeParaRepor(), 1)));
    }

    private void historico() {
        String codigo = entrada.textoObrigatorio("Codigo do produto: ");
        Produto produto = estoque.buscar(codigo);
        List<Movimentacao> lista = estoque.historico(codigo);
        System.out.println("Historico de " + produto + ":");
        if (lista.isEmpty()) {
            System.out.println("  Sem movimentacoes.");
        }
        int saldo = 0;
        for (Movimentacao m : lista) {
            int quantidade = switch (m.tipo()) {
                case ENTRADA -> m.quantidade();
                case SAIDA -> -m.quantidade();
            };
            saldo += quantidade;
            System.out.printf("  %s  %+6d  saldo %5d  %s%n", Formato.dataHora(m.dataHora()), quantidade, saldo,
                    m.observacao() == null ? "" : m.observacao());
        }
        System.out.println("  Saldo atual: " + produto.getEstoqueAtual());
    }

    private void relatorios() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = entrada.data("Data inicial", hoje.withDayOfMonth(1));
        LocalDate fim = entrada.data("Data final", hoje);

        RelatorioService.ResumoVendas resumo = relatorios.resumo(inicio, fim);
        System.out.println();
        System.out.println("Vendas no periodo: " + resumo.quantidadeVendas());
        System.out.println("Faturamento:       " + Formato.moeda(resumo.faturamento()));
        System.out.println("Ticket medio:      " + Formato.moeda(resumo.ticketMedio()));

        System.out.println();
        System.out.println("Faturamento por mes (todas as vendas):");
        Map<YearMonth, BigDecimal> porMes = relatorios.faturamentoPorMes();
        porMes.forEach((mes, valor) -> System.out.printf("  %s  %14s%n", Formato.mes(mes), Formato.moeda(valor)));

        System.out.println();
        System.out.println("Top 5 produtos mais vendidos:");
        List<ProdutoVendido> ranking = relatorios.maisVendidos(5);
        for (int i = 0; i < ranking.size(); i++) {
            ProdutoVendido p = ranking.get(i);
            System.out.printf("  %d. %-30s %5d un.  %14s%n", i + 1, Formato.cortar(p.nome(), 30), p.quantidade(),
                    Formato.moeda(p.faturamento()));
        }

        System.out.println();
        System.out.println("Melhores clientes (sem contar vendas de balcao):");
        relatorios.totalPorCliente().entrySet().stream()
                .filter(e -> !e.getKey().equals("Consumidor"))
                .limit(5)
                .forEach(e -> System.out.printf("  %-30s %14s%n", Formato.cortar(e.getKey(), 30), Formato.moeda(e.getValue())));
    }

    private void exportar() {
        String pasta = entrada.texto("Pasta de destino [exportacao]: ");
        Path destino = Path.of(pasta.isEmpty() ? "exportacao" : pasta);
        exportador.exportarProdutos(estoque.listar(), destino.resolve("produtos.csv"));
        exportador.exportarVendas(vendas.listar(), destino.resolve("vendas.csv"));
        System.out.println("Arquivos gerados em " + destino.toAbsolutePath() + ":");
        System.out.println("  produtos.csv");
        System.out.println("  vendas.csv");
    }
}
