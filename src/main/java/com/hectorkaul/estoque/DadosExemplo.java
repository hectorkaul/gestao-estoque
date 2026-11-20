package com.hectorkaul.estoque;

import com.hectorkaul.estoque.model.Fornecedor;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.service.CompraService;
import com.hectorkaul.estoque.service.EstoqueService;
import com.hectorkaul.estoque.service.ItemPedido;
import com.hectorkaul.estoque.service.VendaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Popula o banco com produtos de uma loja de material de construcao e
 * alguns meses de vendas, para testar os relatorios sem digitar tudo na mao.
 */
public class DadosExemplo {

    private static final String[] CLIENTES = {
            "Consumidor", "Construtora Lopes", "Joao Pereira", "Marcia Schneider",
            "Obra Rua XV", "Paulo Kruger", "Consumidor", "Consumidor"
    };

    private final EstoqueService estoque;
    private final VendaService vendas;
    private final Random random = new Random(42); // semente fixa: sempre gera os mesmos dados

    public DadosExemplo(EstoqueService estoque, VendaService vendas) {
        this.estoque = estoque;
        this.vendas = vendas;
    }

    public void carregar(int meses) {
        List<Produto> produtos = List.of(
                new Produto("CIM50", "Cimento CP-II 50kg", "Basico", dec("29.90"), dec("39.90"), 40),
                new Produto("ARG20", "Argamassa AC-I 20kg", "Basico", dec("14.50"), dec("21.90"), 30),
                new Produto("CAL20", "Cal hidratada 20kg", "Basico", dec("12.00"), dec("17.50"), 20),
                new Produto("TIJ06", "Tijolo 6 furos (milheiro)", "Basico", dec("620.00"), dec("790.00"), 5),
                new Produto("VER10", "Vergalhao CA-50 10mm 12m", "Ferragem", dec("42.00"), dec("58.90"), 25),
                new Produto("PRE25", "Prego 17x27 1kg", "Ferragem", dec("13.80"), dec("19.90"), 15),
                new Produto("TIN18", "Tinta acrilica branca 18L", "Pintura", dec("189.00"), dec("259.90"), 8),
                new Produto("ROL23", "Rolo de la 23cm", "Pintura", dec("14.00"), dec("24.90"), 10),
                new Produto("MAS25", "Massa corrida 25kg", "Pintura", dec("52.00"), dec("74.90"), 8),
                new Produto("TUB25", "Tubo PVC soldavel 25mm 6m", "Hidraulica", dec("18.50"), dec("27.90"), 20),
                new Produto("JOE25", "Joelho PVC 90 graus 25mm", "Hidraulica", dec("0.95"), dec("2.50"), 100),
                new Produto("FIO25", "Fio flexivel 2,5mm rolo 100m", "Eletrica", dec("165.00"), dec("229.90"), 6),
                new Produto("DIS20", "Disjuntor unipolar 20A", "Eletrica", dec("9.80"), dec("16.90"), 15),
                new Produto("LUV01", "Luva de raspa (par)", "EPI", dec("7.50"), dec("14.90"), 12)
        );

        LocalDate inicio = LocalDate.now().minusMonths(meses).withDayOfMonth(1);

        for (Produto produto : produtos) {
            estoque.cadastrar(produto);
            int inicial = produto.getEstoqueMinimo() * (3 + random.nextInt(4));
            estoque.registrarEntrada(produto.getCodigo(), inicial, "Estoque inicial", inicio.atTime(7, 30));
        }

        for (LocalDate dia = inicio; !dia.isAfter(LocalDate.now()); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek().getValue() == 7) {
                continue; // domingo a loja fecha
            }
            int vendasNoDia = 1 + random.nextInt(4);
            for (int i = 0; i < vendasNoDia; i++) {
                registrarVendaAleatoria(produtos, dia);
            }
            if (dia.getDayOfMonth() == 5 || dia.getDayOfMonth() == 20) {
                reporEstoque(produtos, dia.atTime(18, 0));
            }
        }
    }

    private void registrarVendaAleatoria(List<Produto> produtos, LocalDate dia) {
        List<ItemPedido> itens = new ArrayList<>();
        int quantidadeItens = 1 + random.nextInt(3);
        for (int i = 0; i < quantidadeItens; i++) {
            Produto produto = produtos.get(random.nextInt(produtos.size()));
            Produto atual = estoque.buscar(produto.getCodigo());
            int quantidade = 1 + random.nextInt(Math.max(1, atual.getEstoqueMinimo() / 4));
            if (atual.getEstoqueAtual() >= quantidade) {
                itens.add(new ItemPedido(produto.getCodigo(), quantidade));
            }
        }
        if (itens.isEmpty()) {
            return;
        }
        LocalDateTime horario = dia.atTime(LocalTime.of(8 + random.nextInt(10), random.nextInt(60)));
        String cliente = CLIENTES[random.nextInt(CLIENTES.length)];
        try {
            vendas.registrarVenda(cliente, itens, horario);
        } catch (RuntimeException e) {
            // pode acontecer se o mesmo produto saiu duas vezes e somou mais que o estoque; so ignora
        }
    }

    private void reporEstoque(List<Produto> produtos, LocalDateTime quando) {
        for (Produto produto : produtos) {
            Produto atual = estoque.buscar(produto.getCodigo());
            if (atual.getEstoqueAtual() < atual.getEstoqueMinimo() * 2) {
                estoque.registrarEntrada(atual.getCodigo(), atual.getEstoqueMinimo() * 3, "Pedido ao fornecedor", quando);
            }
        }
    }

    /** Fornecedores de exemplo, para testar o menu de compras. */
    public static void cadastrarFornecedores(CompraService compras) {
        compras.cadastrarFornecedor(new Fornecedor("Distribuidora Sul Cimentos", "(53) 3200-1100", 2));
        compras.cadastrarFornecedor(new Fornecedor("Hidraulica e Eletrica Pampa", "vendas@pampa.exemplo", 5));
        compras.cadastrarFornecedor(new Fornecedor("Tintas Litoral", "(51) 3300-2200", 7));
    }

    private static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }
}
