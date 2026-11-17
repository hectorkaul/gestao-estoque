package com.hectorkaul.estoque.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Le o que o usuario digita e repete a pergunta ate vir um valor valido. */
public class Entrada {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Scanner scanner;

    public Entrada(Scanner scanner) {
        this.scanner = scanner;
    }

    public String texto(String pergunta) {
        System.out.print(pergunta);
        return scanner.nextLine().trim();
    }

    public String textoObrigatorio(String pergunta) {
        while (true) {
            String valor = texto(pergunta);
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("  Campo obrigatorio.");
        }
    }

    public int inteiro(String pergunta, int minimo) {
        while (true) {
            String valor = texto(pergunta);
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo) {
                    return numero;
                }
                System.out.println("  Digite um numero a partir de " + minimo + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Numero invalido.");
            }
        }
    }

    /** Aceita 12,50 ou 12.50 */
    public BigDecimal decimal(String pergunta) {
        while (true) {
            String valor = texto(pergunta).replace("R$", "").trim();
            if (valor.contains(",")) {
                valor = valor.replace(".", "").replace(',', '.');
            }
            try {
                BigDecimal numero = new BigDecimal(valor);
                if (numero.signum() >= 0) {
                    return numero;
                }
                System.out.println("  O valor nao pode ser negativo.");
            } catch (NumberFormatException e) {
                System.out.println("  Valor invalido. Exemplo: 12,50");
            }
        }
    }

    public LocalDate data(String pergunta, LocalDate padrao) {
        while (true) {
            String valor = texto(pergunta + " [" + padrao.format(DATA_BR) + "]: ");
            if (valor.isEmpty()) {
                return padrao;
            }
            try {
                return LocalDate.parse(valor, DATA_BR);
            } catch (DateTimeParseException e) {
                System.out.println("  Use o formato dd/mm/aaaa.");
            }
        }
    }

    public boolean simOuNao(String pergunta) {
        String valor = texto(pergunta + " (s/n): ");
        return valor.equalsIgnoreCase("s") || valor.equalsIgnoreCase("sim");
    }
}
