# Gestão de Estoque e Vendas

Sistema de console em Java para controlar estoque e vendas de uma loja pequena. Usei como exemplo uma loja de material de construção, porque é um tipo de negócio que ainda controla muita coisa em caderno ou planilha.

A ideia surgiu do meu dia a dia: no trabalho lido com lançamentos em ERP e planilhas de controle, e queria entender na prática como um sistema desses funciona por dentro, do banco de dados até o relatório.

## O que o sistema faz

- Cadastro de produtos com código, categoria, preço de custo, preço de venda e estoque mínimo
- Entrada de mercadoria e saída manual (perda, quebra, uso interno)
- Registro de vendas com vários itens, com baixa automática no estoque
- Lista de produtos que precisam ser repostos (no mínimo ou abaixo)
- Histórico de cada produto, com saldo linha a linha
- Relatórios: faturamento do período, ticket médio, faturamento por mês, produtos mais vendidos e melhores clientes
- Exportação em CSV no padrão brasileiro (`;` e vírgula decimal), que abre direto no Excel e no Power BI

## Tecnologias

- Java 21
- SQLite (JDBC)
- Maven
- JUnit 5

## Como rodar

Precisa do Java 21 e do Maven.

```bash
mvn package
java -jar target/gestao-estoque.jar --demo
```

O `--demo` cria um banco com 14 produtos e uns 3 meses de vendas geradas automaticamente, para dar para testar os relatórios sem cadastrar tudo na mão. Sem o `--demo`, o sistema começa com o banco vazio. O arquivo do banco é o `estoque.db`, criado na pasta onde o comando rodou.

Para rodar só os testes:

```bash
mvn test
```

## Exemplo

```
Vendas no periodo: 34
Faturamento:       R$ 13.376,40
Ticket medio:      R$ 393,42

Faturamento por mes (todas as vendas):
  08/2026    R$ 26.004,80
  09/2026    R$ 20.803,50
  10/2026    R$ 23.898,60
  11/2026    R$ 13.376,40

Top 5 produtos mais vendidos:
  1. Joelho PVC 90 graus 25mm         503 un.     R$ 1.257,50
  2. Cimento CP-II 50kg               241 un.     R$ 9.615,90
  3. Argamassa AC-I 20kg              127 un.     R$ 2.781,30
```

## Como o código está organizado

```
src/main/java/com/hectorkaul/estoque
├── model        Produto, Venda, ItemVenda, Movimentacao
├── repository   interfaces dos repositórios
│   └── jdbc     implementação com SQL
├── service      regras de negócio (estoque, venda, relatórios)
├── db           conexão e controle de transação
├── ui           menu do console
└── util         exportação CSV
```

O `service` só conhece as interfaces do `repository`, não o JDBC. Se um dia eu trocar o SQLite por MySQL ou PostgreSQL, a regra de negócio não muda.

## Algumas decisões

**Dinheiro em `BigDecimal`.** Com `double`, 0.1 + 0.2 dá 0.30000000000000004. Em valor de venda isso não pode acontecer. No banco os preços ficam como texto, porque o SQLite não tem um tipo decimal exato.

**Venda dentro de uma transação.** Uma venda mexe em três tabelas: baixa o estoque, grava a venda e registra a movimentação. Se der erro no meio, nada fica gravado pela metade. A interface `Transacao` recebe o bloco de código como lambda e faz o commit ou o rollback.

**Validação antes de gravar.** Se um dos itens não tem estoque suficiente, a venda inteira é recusada antes de abrir a transação. Se o mesmo produto for digitado duas vezes, as quantidades são somadas antes de conferir o estoque.

**Ranking no SQL, o resto com Streams.** O "mais vendidos" é um `GROUP BY` direto no banco. O faturamento por mês e por cliente uso `Collectors.groupingBy`, para praticar as duas formas.

## Testes

São 23 testes com JUnit 5. Os de serviço usam um SQLite em memória, criado do zero a cada teste, então testam o SQL de verdade e não precisam de nenhum arquivo. O relógio (`Clock`) é fixo nos testes, assim as datas não mudam conforme o dia em que roda.

## Próximos passos

- [ ] Cadastro de fornecedores e pedido de compra
- [ ] Cancelamento de venda (devolvendo o estoque)
- [ ] Trocar o console por uma API REST com Spring Boot
- [ ] Dashboard no Power BI usando os CSVs exportados
