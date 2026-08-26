# Sistema de Gestão Comercial

Sistema desktop de gestão comercial desenvolvido em Java, com foco em pequenos e médios comércios que precisam controlar produtos, clientes, vendas, estoque, promoções e informações financeiras de forma simples e organizada.

Este projeto foi desenvolvido como parte do meu Trabalho de Conclusão de Curso em Análise e Desenvolvimento de Sistemas.

---

## Objetivo do projeto

O objetivo do sistema é centralizar processos comerciais em uma aplicação desktop, reduzindo controles manuais e auxiliando na organização das principais rotinas de uma loja.

Entre os principais objetivos estão:

- Cadastrar e gerenciar usuários, clientes e produtos;
- Controlar estoque, promoções e prazos de pagamento;
- Registrar vendas à vista e a prazo;
- Controlar contas a receber e movimentações financeiras;
- Emitir Nota de Venda em PDF;
- Consultar histórico, relatórios e informações gerenciais.

---

## Tecnologias utilizadas

- Java 17
- JavaFX
- FXML
- SQLite
- JDBC puro
- Maven
- BCrypt / jBCrypt
- Apache PDFBox
- Scene Builder
- Git e GitHub

---

## Arquitetura do projeto

O sistema utiliza uma organização em camadas, separando interface, regras de negócio, acesso a dados e entidades do domínio.

O fluxo principal segue a arquitetura:

```text
Controller → Service → DAO
```

Estrutura principal:

```text
src/main/java/br/com/luis/
├── controller/
├── service/
├── dao/
├── model/
├── util/
└── viewmodel/

src/main/resources/br/com/luis/view/
└── arquivos FXML

src/main/resources/database/
└── scripts SQL
```

Os valores monetários são tratados com `BigDecimal`, evitando perda de precisão em preços, descontos, totais, contas e movimentações financeiras.

---

## Funcionalidades implementadas

- Autenticação e sessão de usuário;
- Perfis de acesso `ADMIN` e `VENDEDOR`;
- Cadastro, consulta, edição, ativação e inativação de usuários;
- Cadastro e consulta de clientes Pessoa Física e Pessoa Jurídica;
- Validação de CPF e CNPJ;
- Situação financeira do cliente com saldo devedor e limite disponível;
- Cadastro, consulta e edição de produtos, com Preço de Venda e Estoque Mínimo cadastral;
- Último Preço de Compra somente leitura, obtido do histórico de Entradas de Estoque, sem novo campo persistido em Produto;
- Quantidade Inicial para produto novo e Estoque Atual somente para consulta em produto existente;
- Ativação e inativação de produtos por Status + Atualizar;
- Entrada de estoque rastreável, com múltiplos produtos e Referência interna opcional;
- Registro de Quantidade Recebida e Preço Unitário de Compra;
- Rascunho com ação contextual Adicionar Item / Atualizar Item;
- Atualizar Item altera somente o rascunho;
- Confirmar Entrada registra a operação inteira e incrementa o estoque;
- Alterações do editor ainda não aplicadas por Atualizar Item não são incorporadas silenciosamente na confirmação;
- Histórico de entradas com responsável, referência e observação;
- Relatório de Entradas de Estoque com filtros, detalhes e totalizadores;
- Cadastro e gerenciamento de promoções;
- Cadastro e gerenciamento de prazos de pagamento;
- Carrinho de venda com alteração de quantidade;
- Promoções automáticas e desconto global por valor ou percentual;
- Venda à vista com dinheiro, PIX ou cartão;
- Cálculo de troco para pagamentos em dinheiro;
- Venda a prazo com cliente, prazo e validação de limite de crédito;
- Geração de contas a receber;
- Recebimento integral de contas por usuário administrador;
- Movimentações financeiras de entrada e saída;
- Nota de Venda em PDF, segunda via e histórico;
- Estorno total de vendas com restauração de estoque;
- Histórico de Vendas com filtros e detalhes;
- Relatórios e consultas gerenciais;
- Dashboard com informações de vendas, recebimentos, pendências e estoque baixo;
- Alertas de contas vencidas e próximas do vencimento;
- Logout com encerramento da sessão.

---

## Regras de negócio trabalhadas

O projeto busca representar regras comuns de um sistema comercial, como:

- Não permitir venda de produto sem estoque suficiente;
- Produtos existentes não têm saldo alterado pelo cadastro;
- A reposição normal do saldo ocorre pela Entrada de Estoque;
- Entradas aceitam somente produtos ativos;
- A confirmação da Entrada persiste cabeçalho, itens e incrementos no mesmo commit;
- O histórico de Entradas preserva snapshots do responsável e dos produtos;
- O filtro do relatório por produto seleciona a Entrada inteira;
- Aplicar promoções ativas automaticamente;
- Não aplicar desconto global sobre itens promocionais;
- Validar limite de crédito em vendas a prazo;
- Respeitar o prazo máximo definido para o cliente;
- Manter venda, itens, estoque, contas, movimentações e Nota de Venda consistentes em transações;
- Impedir recebimento duplicado de contas;
- Impedir estorno duplicado;
- Preservar registros financeiros e históricos;
- Restringir operações administrativas conforme o perfil do usuário.

---

## Banco de dados

O sistema utiliza SQLite como banco de dados local.

Alguns conceitos aplicados:

- Tabelas relacionais;
- Chaves primárias e estrangeiras;
- Scripts SQL;
- Persistência com JDBC;
- Uso de `PreparedStatement`;
- Transações com commit e rollback;
- Separação entre regra de negócio e acesso ao banco.

---

## Status do projeto

O sistema está funcionalmente concluído para o escopo definido no TCC.

Os principais módulos de autenticação, usuários, clientes, produtos, estoque, promoções, prazos, vendas, contas a receber, movimentações financeiras, Nota de Venda, estorno, histórico, relatórios, dashboard e alertas estão implementados.

O R6 foi concluído com a Entrada de Estoque e o Relatório de Entradas de Estoque implementados.

A regressão global pós-R6 foi concluída sem defeitos funcionais bloqueantes reproduzidos.

O fechamento R6.12 consolidou a regressão global pós-R6 e preservou o histórico técnico dessa evolução.

Os refinamentos posteriores também foram concluídos:

- R6.13 — refinamento do Cadastro de Produtos e Último Preço de Compra;
- R6.13A — aderência visual final do Cadastro de Produtos;
- R6.14 — refinamento visual e clareza da Entrada de Estoque.

HEAD técnico anterior a esta atualização documental: `28da89fa092748170f0bffd1618186c4c7f8d341`.

Último commit técnico anterior: `fix: alinhar entrada de estoque ao fluxo aprovado`.

---

## Como executar o projeto

### Pré-requisitos

- Java 17 instalado;
- Maven instalado ou configurado pela IDE;
- IntelliJ IDEA ou outra IDE compatível.

### Passos básicos

1. Clone o repositório:

```bash
git clone https://github.com/felipe57863/sistema-gestao-comercial.git
```

2. Abra o projeto em uma IDE Java.

3. Aguarde o Maven baixar as dependências.

4. Execute a classe `Launcher`.

5. Após a autenticação, utilize a Tela Principal para acessar os módulos disponíveis para o perfil do usuário.

---

## Aprendizados aplicados

Durante o desenvolvimento deste projeto, foram praticados conceitos como:

- Programação orientada a objetos;
- Desenvolvimento desktop com JavaFX;
- Criação de interfaces com FXML;
- Organização em camadas;
- Validação de regras de negócio;
- Integração com banco de dados SQLite;
- Manipulação de dados com JDBC;
- Uso de transações;
- Segurança de senhas com BCrypt;
- Geração de arquivos PDF;
- Versionamento com Git e GitHub;
- Modelagem de banco de dados;
- Desenvolvimento incremental e testes de regressão.

---

## Autor

**Luís Felipe Bueno**

Estudante de Análise e Desenvolvimento de Sistemas.

- LinkedIn: [linkedin.com/in/luis-felipe-bueno](https://www.linkedin.com/in/luis-felipe-bueno)
- GitHub: [github.com/felipe57863](https://github.com/felipe57863)
