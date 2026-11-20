package com.hectorkaul.estoque.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Formato {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private Formato() {
    }

    static String moeda(BigDecimal valor) {
        // o NumberFormat usa espaco especial depois do R$, troco por espaco normal pro console
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }

    static String data(LocalDate data) {
        return data.format(DATA);
    }

    static String dataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }

    static String mes(YearMonth mes) {
        return mes.format(MES);
    }

    static String cortar(String texto, int tamanho) {
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho - 1) + ".";
    }
}
