# Bills Manager Bot - Bot de Controle Financeiro para Telegram

Este repositório contém o backend de um bot para o Telegram focado no gerenciamento de despesas pessoais. O projeto é dividido em duas partes principais: o bot que interage com o usuário, desenvolvido em Java e rodando em uma função AWS Lambda, e uma API de processamento em Go, responsável por registrar as despesas em uma planilha do Google Sheets de forma assíncrona.

## ✨ Funcionalidades

* **Adicionar Despesas:** Um fluxo de conversa completo para registrar valor, descrição, método de pagamento e categoria.
* **Listar Despesas:** Exibe um resumo de todas as despesas registradas pelo usuário.
* **Atualizar Despesas:** Permite que o usuário edite informações de uma despesa existente através de um ID.
* **Deletar Despesas:** Remove uma despesa específica.
* **Integração com Google Sheets:** Vincula uma planilha pessoal do Google Sheets à conta do usuário para registro automático das despesas.
* **Processamento Assíncrono:** As despesas são enviadas para uma fila (RabbitMQ), garantindo que o registro na planilha seja feito de forma segura e sem bloquear a resposta do bot.

## 🏛️ Arquitetura

O sistema é composto por dois microsserviços que se comunicam de forma desacoplada:

### 1. **Bot de Telegram (AWS Lambda - Java)**
* Responsável por toda a interação com o usuário através da API do Telegram.
* Gerencia a máquina de estados da conversa (ex: aguardando valor, aguardando descrição).
* Utiliza o **AWS DynamoDB** para persistir os dados do usuário, o estado da conversa e as despesas em andamento.
* Ao final do registro de uma despesa, consome a API em Go para iniciar o processo de escrita na planilha.

### 2. **API de Planilhas (Go - RabbitMQ)**
* Uma API simples em Go que expõe um endpoint para receber os dados da despesa finalizada.
* Ao receber uma nova despesa, a publica em uma fila no **RabbitMQ**.
* Um *worker* (também em Go) consome as mensagens da fila de forma assíncrona.
* O worker utiliza as credenciais da API do **Google Sheets** para adicionar a despesa como uma nova linha na planilha vinculada pelo usuário.

## 🚀 Tecnologias Utilizadas

* **Linguagens:** Java 21, Go (Golang)
* **Plataforma:** AWS Lambda (Serverless)
* **Banco de Dados:** AWS DynamoDB (NoSQL)
* **Mensageria:** RabbitMQ
* **API (Go):** Biblioteca `net/http` e Roteador `Chi`
* **Integração:** Google Sheets API
* **Interface:** Telegram Bot API
