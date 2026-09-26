# Sistema de Gestão Comercial

Sistema desktop de gestão comercial desenvolvido em **Java 17 + JavaFX**, voltado a lojas de pequeno e médio porte.

A aplicação centraliza rotinas de produtos, clientes, estoque, vendas, promoções, contas a receber, movimentações financeiras, histórico e relatórios, buscando reduzir controles manuais e manter consistência entre as principais operações comerciais.

O projeto foi desenvolvido como **Trabalho de Conclusão de Curso em Análise e Desenvolvimento de Sistemas**.

---

## Objetivo

O objetivo do sistema é disponibilizar uma aplicação desktop capaz de apoiar as principais atividades operacionais e gerenciais de um estabelecimento comercial.

Entre os processos contemplados estão:

- gestão de usuários e perfis de acesso;
- cadastro de clientes e produtos;
- controle de estoque;
- entrada de mercadorias;
- promoções e descontos;
- vendas à vista e a prazo;
- controle de contas a receber;
- movimentações financeiras;
- emissão de Nota de Venda não fiscal;
- estorno de vendas;
- histórico e relatórios gerenciais.

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
- Git
- GitHub

---

## Arquitetura

O sistema utiliza uma arquitetura em camadas, separando interface, regras de negócio e persistência.

O fluxo principal é:

```text
Controller → Service → DAO
```

### Responsabilidades

```text
Controller
├── interação com a interface JavaFX
├── eventos
├── navegação
└── apresentação dos dados

Service
├── validações
├── regras de negócio
├── cálculos
├── autorização de operações
└── controle de transações

DAO
├── SQL
├── JDBC
├── PreparedStatement
├── consultas
└── persistência
```

Estrutura principal do projeto:

```text
src/main/java/br/com/luis/
├── controller/
├── service/
├── dao/
├── model/
├── util/
└── viewmodel/

src/main/resources/
├── br/com/luis/view/
│   └── arquivos FXML
│
└── database/
    └── scripts SQL
```

---

## Persistência e transações

O banco de dados utilizado é o **SQLite**, acessado diretamente por JDBC, sem ORM.

Entre os conceitos aplicados estão:

- JDBC puro;
- `PreparedStatement`;
- chaves primárias e estrangeiras;
- `PRAGMA foreign_keys = ON`;
- `try-with-resources`;
- transações com `commit` e `rollback`;
- uso da mesma `Connection` em operações compostas;
- preservação de consistência entre registros relacionados.

Operações críticas, como vendas, recebimentos, entradas de estoque, estornos e operações envolvendo produto e promoção, são tratadas de forma transacional.

Valores monetários são manipulados com `BigDecimal`, evitando o uso de `double` ou `float` em preços, descontos, totais e movimentações financeiras.

---

## Funcionalidades

### Autenticação e usuários

- autenticação por login e senha;
- senhas protegidas com BCrypt;
- perfis `ADMIN` e `VENDEDOR`;
- cadastro e edição de usuários;
- ativação e inativação;
- redefinição administrativa de senha;
- troca obrigatória de senha;
- controle de sessão;
- logout.

### Clientes

- cadastro de Pessoa Física e Pessoa Jurídica;
- validação de CPF e CNPJ;
- consulta e edição;
- ativação e inativação;
- definição de limite de crédito;
- definição de prazo máximo permitido;
- consulta de saldo devedor e limite disponível.

### Produtos

- cadastro e edição;
- ativação e inativação;
- bloqueio de cadastro duplicado por descrição;
- preço de venda;
- estoque mínimo;
- quantidade inicial no primeiro cadastro;
- estoque atual somente para consulta em produtos existentes;
- último preço de compra obtido pelo histórico de entradas de estoque;
- pesquisa e listagem de produtos.

### Promoções

- cadastro de promoções;
- desconto percentual;
- desconto em valor fixo;
- somente uma promoção ativa por produto;
- substituição automática da promoção anterior;
- aplicação automática durante a venda.

### Entrada de estoque

- registro de entradas com múltiplos produtos;
- quantidade recebida;
- preço unitário de compra;
- referência interna opcional;
- observações;
- edição do rascunho antes da confirmação;
- atualização do estoque somente após a confirmação;
- persistência transacional da entrada e de seus itens;
- histórico de entradas;
- identificação do usuário responsável.

### Prazos de pagamento

- cadastro e gerenciamento de prazos;
- utilização em vendas a prazo;
- validação do prazo selecionado em relação ao limite definido para o cliente.

### Vendas

- carrinho de produtos;
- alteração de quantidades;
- validação de estoque disponível;
- promoção automática;
- desconto global por valor ou percentual;
- exclusão de itens promocionais do desconto global;
- venda à vista;
- venda a prazo.

#### Venda à vista

Formas de pagamento disponíveis:

- dinheiro;
- PIX;
- cartão.

Para pagamento em dinheiro, o sistema realiza o cálculo de troco.

Após a finalização:

- a venda é registrada;
- os itens são persistidos;
- o estoque é atualizado;
- a movimentação financeira é registrada;
- a Nota de Venda é gerada.

#### Venda a prazo

A venda a prazo exige:

- cliente ativo;
- prazo válido;
- limite de crédito disponível.

Após a finalização:

- a venda permanece pendente;
- o estoque é atualizado;
- uma Conta a Receber é gerada;
- não ocorre entrada financeira imediata.

---

## Contas a receber

O sistema realiza o controle das contas geradas pelas vendas a prazo, incluindo:

- geração automática em vendas a prazo;
- consulta de contas pendentes;
- recebimento integral;
- registro da forma de pagamento;
- atualização da situação da venda;
- geração da movimentação financeira correspondente;
- bloqueio de recebimento duplicado;
- cancelamento da conta em caso de estorno da venda.

---

## Movimentações financeiras

O sistema registra movimentações decorrentes das operações comerciais.

São contempladas:

- entrada por venda à vista;
- entrada por recebimento de Conta a Receber;
- saída compensatória em estornos, quando aplicável.

As movimentações financeiras são preservadas para fins históricos e de rastreabilidade.

O módulo representa as movimentações financeiras do sistema e **não corresponde a um controle físico de caixa**.

---

## Nota de Venda

Cada venda finalizada gera uma **Nota de Venda interna e não fiscal**.

O sistema permite:

- geração automática da nota;
- emissão em PDF;
- segunda via;
- consulta pelo histórico;
- preservação do mesmo número e dos dados originais;
- identificação de notas relacionadas a vendas estornadas.

A Nota de Venda não substitui documentos fiscais como NF-e ou NFC-e.

---

## Estorno de vendas

O estorno é restrito ao perfil administrador.

A operação:

- estorna integralmente a venda;
- restaura as quantidades dos produtos ao estoque;
- impede estorno duplicado;
- preserva a venda original para histórico;
- preserva a Nota de Venda;
- cancela a Conta a Receber associada, quando aplicável;
- gera movimentação financeira compensatória, quando necessário.

Os registros financeiros originais não são apagados.

---

## Histórico e relatórios

O sistema disponibiliza histórico e consultas gerenciais, incluindo:

- Histórico de Vendas;
- produtos e estoque;
- promoções;
- Contas a Receber;
- movimentações financeiras;
- entradas de estoque;
- clientes com pendências;
- descontos concedidos.

Os relatórios possuem filtros e informações adequadas ao respectivo contexto.

---

## Dashboard e alertas

A Tela Principal apresenta informações resumidas sobre a operação do sistema, incluindo:

- vendas;
- recebimentos;
- contas pendentes;
- produtos com estoque abaixo ou igual ao mínimo.

Também são disponibilizados alertas relacionados a:

- contas vencidas;
- contas próximas do vencimento.

---

## Principais regras de negócio

Entre as regras implementadas estão:

- não permitir venda acima do estoque disponível;
- somente produtos ativos podem ser utilizados em novas vendas e entradas;
- produtos já cadastrados não têm seu saldo alterado diretamente pela edição cadastral;
- reposições de estoque são realizadas pelo módulo de Entrada de Estoque;
- promoções ativas são aplicadas automaticamente;
- um produto possui no máximo uma promoção ativa;
- itens promocionais não recebem desconto global;
- vendas a prazo exigem cliente e prazo válidos;
- o prazo selecionado não pode ultrapassar o máximo permitido ao cliente;
- o limite de crédito considera valores pendentes existentes e a nova venda;
- vendas à vista geram movimentação financeira imediatamente;
- vendas a prazo geram Conta a Receber;
- o recebimento de uma conta ocorre integralmente;
- recebimentos duplicados são impedidos;
- estornos duplicados são impedidos;
- o estorno restaura o estoque;
- Contas a Receber relacionadas a vendas estornadas são canceladas;
- movimentações financeiras não são apagadas para desfazer operações;
- operações compostas utilizam transações para preservar consistência;
- operações administrativas são protegidas conforme o perfil do usuário.

---

## Como executar

### Pré-requisitos

- Java 17;
- Maven;
- IntelliJ IDEA ou outra IDE compatível com JavaFX.

### Clonar o repositório

```bash
git clone https://github.com/felipe57863/sistema-gestao-comercial.git
```

Entre no diretório do projeto:

```bash
cd sistema-gestao-comercial
```

### Compilar

```bash
mvn clean compile
```

### Executar

Abra o projeto na IDE, aguarde o Maven resolver as dependências e execute a classe:

```text
br.com.luis.Launcher
```

O banco SQLite local é criado e configurado pela própria aplicação a partir dos scripts disponíveis em:

```text
src/main/resources/database/
```

---

## Status

O sistema encontra-se **funcionalmente concluído para o escopo definido no Trabalho de Conclusão de Curso**.

---

## Aprendizados aplicados

Durante o desenvolvimento foram aplicados conceitos de:

- programação orientada a objetos;
- Java desktop com JavaFX;
- interfaces FXML;
- arquitetura em camadas;
- JDBC;
- SQL e SQLite;
- transações;
- regras de negócio;
- validação de dados;
- segurança de senhas com BCrypt;
- geração de PDF;
- modelagem de banco de dados;
- Git e GitHub;
- desenvolvimento incremental;
- testes funcionais e regressão.

---

## Autor

**Luís Felipe Bueno**

Estudante de Análise e Desenvolvimento de Sistemas.

- LinkedIn: [linkedin.com/in/luis-felipe-bueno](https://www.linkedin.com/in/luis-felipe-bueno)
- GitHub: [github.com/felipe57863](https://github.com/felipe57863)