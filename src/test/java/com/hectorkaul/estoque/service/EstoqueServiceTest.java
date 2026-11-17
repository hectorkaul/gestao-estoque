package com.hectorkaul.estoque.service;

import com.hectorkaul.estoque.exception.RegraDeNegocioException;
import com.hectorkaul.estoque.model.Movimentacao;
import com.hectorkaul.estoque.model.Produto;
import com.hectorkaul.estoque.model.TipoMovimentacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueServiceTest {

    private final BancoDeTeste banco = new BancoDeTeste();

    @AfterEach
    void fechar() {
        banco.close();
    }

    @Test
    void naoDeveCadastrarDoisProdutosComOMesmoCodigo() {
        banco.cadastrar("A1", "10.00", 0, 0);

        assertThrows(RegraDeNegocioException.class, () -> banco.cadastrar("a1", "20.00", 0, 0));
    }

    @Test
    void entradaESaidaAtualizamOEstoqueEGuardamHistorico() {
        banco.cadastrar("A1", "10.00", 0, 0);

        banco.estoque.registrarEntrada("A1", 20, "NF 123");
        banco.estoque.registrarSaida("A1", 3, "Saco rasgado");

        assertEquals(17, banco.estoque.buscar("A1").getEstoqueAtual());

        List<Movimentacao> historico = banco.estoque.historico("A1");
        assertEquals(2, historico.size());
        assertEquals(TipoMovimentacao.ENTRADA, historico.get(0).tipo());
        assertEquals(TipoMovimentacao.SAIDA, historico.get(1).tipo());
        assertEquals(BancoDeTeste.AGORA, historico.get(0).dataHora());
    }

    @Test
    void saidaMaiorQueOEstoqueNaoAlteraNada() {
        banco.cadastrar("A1", "10.00", 5, 0);

        assertThrows(RegraDeNegocioException.class, () -> banco.estoque.registrarSaida("A1", 6, "teste"));

        assertEquals(5, banco.estoque.buscar("A1").getEstoqueAtual());
        assertEquals(1, banco.estoque.historico("A1").size());
    }

    @Test
    void produtosParaReporVemOrdenadosPeloQueMaisFalta() {
        banco.cadastrar("A1", "10.00", 8, 10);  // faltam 2
        banco.cadastrar("B2", "10.00", 1, 20);  // faltam 19
        banco.cadastrar("C3", "10.00", 50, 5);  // ok

        List<Produto> repor = banco.estoque.produtosParaRepor();

        assertEquals(List.of("B2", "A1"), repor.stream().map(Produto::getCodigo).toList());
    }

    @Test
    void valorEmEstoqueSomaCustoVezesQuantidade() {
        banco.cadastrar("A1", "10.00", 10, 0); // custo 7,00
        banco.cadastrar("B2", "20.00", 5, 0);  // custo 14,00

        assertEquals(0, new BigDecimal("140.00").compareTo(banco.estoque.valorTotalEmEstoque()));
    }

    @Test
    void produtoInexistenteDaMensagemClara() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> banco.estoque.buscar("NAOEXISTE"));
        assertEquals("Produto NAOEXISTE nao encontrado.", erro.getMessage());
    }
}
