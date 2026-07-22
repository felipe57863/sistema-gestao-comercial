# Sistema de Gestão Comercial

Sistema desktop de gestão comercial desenvolvido em Java, com foco em pequenos comércios que precisam controlar produtos, clientes, vendas, estoque, promoções e informações financeiras de forma simples e organizada.

Este projeto está sendo desenvolvido como parte do meu Trabalho de Conclusão de Curso em Análise e Desenvolvimento de Sistemas.

---

## Objetivo do projeto

O objetivo do sistema é centralizar processos comerciais em uma aplicação desktop, reduzindo controles manuais e auxiliando na organização das principais rotinas de uma loja.

Entre os principais objetivos estão:

- Cadastrar e gerenciar produtos;
- Controlar clientes e seus dados comerciais;
- Registrar vendas;
- Aplicar regras de estoque;
- Trabalhar com promoções e descontos;
- Apoiar o controle financeiro;
- Gerar informações úteis para tomada de decisão.

---

## Tecnologias utilizadas

- Java 17
- JavaFX
- FXML
- SQLite
- JDBC puro
- Maven
- BCrypt/jBCrypt
- Scene Builder
- Git e GitHub

---

## Arquitetura do projeto

O sistema utiliza uma organização em camadas, separando responsabilidades entre interface, regras de negócio, acesso a dados e entidades do domínio. O fluxo principal segue a arquitetura `Controller → Service → DAO`.

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

### Camadas

* `controller`: controla a interface e coordena as ações do usuário, delegando as regras aos Services;
* `service`: concentra regras de negócio, validações, cálculos e controle transacional;
* `dao`: realiza consultas e persistência com JDBC e `PreparedStatement`;
* `model`: representa as entidades e os tipos do domínio;
* `util`: reúne recursos auxiliares de conexão, banco, sessão, navegação e cabeçalho;
* `viewmodel`: transporta dados preparados para apresentação nas telas;
* `src/main/resources/br/com/luis/view`: contém as telas JavaFX definidas em FXML.

Os valores monetários são tratados com `BigDecimal`, evitando perda de precisão em preços, descontos, totais, contas e movimentações financeiras.

### Fluxo principal de inicialização

```text
Launcher
→ Main
→ preparação do banco e dos dados iniciais
→ Login.fxml
→ TelaPrincipal.fxml
```

---

## Funcionalidades

### Recursos implementados

* Autenticação e sessão de usuário;
* Cadastro e consulta de usuários;
* Cadastro e consulta de clientes e produtos;
* Cadastro e consulta de prazos de pagamento e promoções;
* Controle de estoque;
* Carrinho de venda, alteração de quantidade e desconto global;
* Venda à vista com pagamento em dinheiro, PIX ou cartão e cálculo de troco;
* Venda a prazo com seleção de cliente e prazo e validação de limite de crédito;
* Persistência transacional da venda e dos itens, com baixa de estoque na finalização;
* Movimentação financeira para venda à vista;
* Geração e consulta de contas a receber para vendas a prazo;
* Recebimento integral de conta com geração da movimentação financeira correspondente;
* Estorno total de vendas, com restauração de estoque e cancelamento da conta a receber;
* Movimentação financeira de saída quando necessária no estorno;
* Auditoria de estorno e bloqueio de estorno duplicado;
* Histórico de Vendas com filtros, detalhes e estorno contextual pela própria tela.

### Próximas evoluções

* Dashboard gerencial;
* Relatórios;
* Geração de documento ou nota de venda em PDF;
* Refinamentos finais de interface, documentação e qualidade.

---

## Regras de negócio trabalhadas

O projeto busca simular regras reais de um sistema comercial, como:

* Não permitir venda de produto sem estoque;
* Controlar produtos ativos e inativos;
* Validar dados obrigatórios antes de salvar registros;
* Aplicar promoções ativas automaticamente;
* Separar desconto promocional de desconto global;
* Impedir inconsistências no cálculo de valores da venda;
* Validar limite de crédito e prazo nas vendas a prazo;
* Manter venda, itens, estoque, contas e movimentações consistentes em transações;
* Impedir estorno duplicado e manter sua auditoria;
* Organizar responsabilidades entre as camadas do sistema.

---

## Banco de dados

O sistema utiliza SQLite como banco de dados local.

Alguns conceitos aplicados:

* Tabelas relacionais;
* Chaves primárias e estrangeiras;
* Scripts SQL;
* Persistência com JDBC;
* Uso de `PreparedStatement`;
* Separação entre regra de negócio e acesso ao banco.

---

## Status do projeto

O sistema já possui os módulos centrais de autenticação, cadastros, estoque, vendas à vista e a prazo, contas a receber, movimentações financeiras, estorno e Histórico de Vendas operacionais.

As próximas evoluções previstas concentram-se no dashboard gerencial, relatórios, geração de documento ou nota de venda em PDF e refinamentos finais de interface, documentação e qualidade.

---

## Como executar o projeto

### Pré-requisitos

* Java 17 instalado;
* Maven instalado ou configurado pela IDE;
* IntelliJ IDEA ou outra IDE compatível;
* Scene Builder, caso deseje editar as telas FXML.

### Passos básicos

1. Clone o repositório:

```bash
git clone https://github.com/felipe57863/sistema-gestao-comercial.git
```

2. Abra o projeto em uma IDE Java, como IntelliJ IDEA.

3. Aguarde o Maven baixar as dependências.

4. Execute a classe `Launcher`, que inicia `Main`, prepara o banco e os dados iniciais e abre a tela de login.

5. Após a autenticação, utilize a Tela Principal para acessar os módulos disponíveis.

---

## Aprendizados aplicados

Durante o desenvolvimento deste projeto, foram praticados conceitos como:

* Programação orientada a objetos;
* Desenvolvimento desktop com JavaFX;
* Criação de interfaces com FXML;
* Organização em camadas;
* Validação de regras de negócio;
* Integração com banco de dados SQLite;
* Manipulação de dados com JDBC;
* Versionamento com Git;
* Modelagem de banco de dados;
* Desenvolvimento incremental de funcionalidades.

---

## Autor

Luís Felipe Bueno

Estudante de Análise e Desenvolvimento de Sistemas.

* LinkedIn: [linkedin.com/in/luis-felipe-bueno](https://www.linkedin.com/in/luis-felipe-bueno)
* GitHub: [github.com/felipe57863](https://github.com/felipe57863)