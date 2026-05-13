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
- JDBC
- Maven
- Scene Builder
- Git e GitHub

---

## Arquitetura do projeto

O sistema utiliza uma organização em camadas, separando responsabilidades entre interface, regras de negócio, acesso a dados e entidades do domínio.

Estrutura principal:

src/main/java/br/com/luis/
├── controller
├── dao
├── model
├── service
├── util
└── view

### Camadas

* `model`: representa as entidades do sistema;
* `dao`: responsável pela comunicação com o banco de dados;
* `service`: concentra regras de negócio e validações;
* `controller`: controla a interação entre interface e sistema;
* `view`: telas desenvolvidas com JavaFX/FXML;
* `util`: classes auxiliares, como conexão com banco de dados.

---

## Funcionalidades

### Implementadas ou em desenvolvimento

* Cadastro de produtos;
* Cadastro de clientes;
* Cadastro de prazos de pagamento;
* Controle de promoções;
* Validações de regras de negócio;
* Estrutura de vendas;
* Carrinho de vendas em memória;
* Aplicação de promoções;
* Cálculo de subtotal e total da venda;
* Persistência com SQLite.

### Funcionalidades planejadas

* Finalização de vendas;
* Controle financeiro;
* Contas a receber;
* Relatórios;
* Estorno de venda;
* Controle de permissões por perfil de usuário.

---

## Regras de negócio trabalhadas

O projeto busca simular regras reais de um sistema comercial, como:

* Não permitir venda de produto sem estoque;
* Controlar produtos ativos e inativos;
* Validar dados obrigatórios antes de salvar registros;
* Aplicar promoções ativas automaticamente;
* Separar desconto promocional de desconto global;
* Impedir inconsistências no cálculo de valores da venda;
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

Projeto em desenvolvimento.

Atualmente, o foco está na construção do motor de vendas, incluindo carrinho, itens da venda, promoções, descontos e cálculo de totais.

---

## Como executar o projeto

### Pré-requisitos

* Java 17 instalado;
* Maven instalado ou configurado pela IDE;
* IntelliJ IDEA ou outra IDE compatível;
* SQLite;
* Scene Builder, caso deseje editar as telas FXML.

### Passos básicos

1. Clone o repositório:

git clone https://github.com/felipe57863/sistema-gestao-comercial.git

2. Abra o projeto em uma IDE Java, como IntelliJ IDEA.

3. Aguarde o Maven baixar as dependências.

4. Configure o banco SQLite conforme os scripts do projeto.

5. Execute a classe principal da aplicação.

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