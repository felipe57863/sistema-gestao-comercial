package br.com.luis.controller;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.Usuario;
import br.com.luis.service.ContaReceberService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.ContaReceberListagemView;
import br.com.luis.viewmodel.ResultadoRecebimentoConta;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableRow;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller da tela de Contas a Receber.
 *
 * O Controller configura a tabela, carrega as contas pendentes,
 * preenche o painel da conta selecionada, controla a navegação
 * e executa o fluxo visual de recebimento integral da conta.
 */
public class ContasReceberController {

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    @FXML private Label lblContaId;
    @FXML private Label lblCliente;
    @FXML private Label lblVendaId;
    @FXML private Label lblValor;
    @FXML private Label lblVencimento;
    @FXML private Label lblStatus;
    @FXML private Label lblSituacao;

    @FXML private Button btnVoltar;
    @FXML private Button btnReceberConta;
    @FXML private Button btnAtualizar;

    @FXML private TableView<ContaReceberListagemView> tabelaContasPendentes;

    @FXML private TableColumn<ContaReceberListagemView, Integer> colConta;
    @FXML private TableColumn<ContaReceberListagemView, String> colCliente;
    @FXML private TableColumn<ContaReceberListagemView, Integer> colVenda;
    @FXML private TableColumn<ContaReceberListagemView, BigDecimal> colValor;
    @FXML private TableColumn<ContaReceberListagemView, LocalDate> colVencimento;
    @FXML private TableColumn<ContaReceberListagemView, StatusContaReceber> colStatus;
    @FXML private TableColumn<ContaReceberListagemView, String> colSituacao;

    @FXML private Label lblTotalContas;

    private final ContaReceberService contaReceberService;
    private final ObservableList<ContaReceberListagemView> contasPendentes;

    /**
     * Construtor do Controller.
     *
     * Inicializa o Service e a lista observável usada pela TableView.
     * Não acessa banco de dados.
     */
    public ContasReceberController() {
        this.contaReceberService = new ContaReceberService();
        this.contasPendentes = FXCollections.observableArrayList();
    }

    /**
     * Inicialização da tela.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );
        btnReceberConta.setDisable(true);
        configurarTabela();
        configurarSelecaoTabela();
        limparPainelContaSelecionada();
        carregarContasPendentes();
    }

    /**
     * Configura as colunas da TableView de contas pendentes.
     *
     * A formatação visual permanece no Controller.
     */
    private void configurarTabela() {

        colConta.setCellValueFactory(new PropertyValueFactory<>("contaReceberId"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colVenda.setCellValueFactory(new PropertyValueFactory<>("vendaId"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colVencimento.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSituacao.setCellValueFactory(cellData -> {
            ContaReceberListagemView contaPendente = cellData.getValue();

            if (contaPendente == null) {
                return new SimpleStringProperty("-");
            }

            String situacao = contaPendente.isVencida()
                    ? "Vencida"
                    : "Em aberto";

            return new SimpleStringProperty(situacao);
        });

        configurarFormatacaoColunaValor();
        configurarFormatacaoColunaVencimento();
        configurarDestaqueContasVencidas();

        tabelaContasPendentes.setItems(contasPendentes);
    }

    /**
     * Configura o destaque visual das contas vencidas na tabela.
     *
     * A regra de vencimento já vem calculada pelo Service.
     * O Controller apenas aplica a apresentação visual.
     */
    private void configurarDestaqueContasVencidas() {

        tabelaContasPendentes.setRowFactory(tabela -> {

            TableRow<ContaReceberListagemView> linha = new TableRow<>() {
                @Override
                protected void updateItem(
                        ContaReceberListagemView conta,
                        boolean empty
                ) {
                    super.updateItem(conta, empty);
                    atualizarEstiloLinha(this, conta);
                }
            };

            linha.selectedProperty().addListener(
                    (observable, selecaoAnterior, selecionada) ->
                            atualizarEstiloLinha(linha, linha.getItem())
            );

            return linha;
        });
    }

    /**
     * Atualiza o estilo visual de uma linha da tabela.
     *
     * Linhas vazias, contas não vencidas e linhas selecionadas
     * não recebem estilo inline.
     */
    private void atualizarEstiloLinha(
            TableRow<ContaReceberListagemView> linha,
            ContaReceberListagemView conta
    ) {

        if (linha.isEmpty() || conta == null || linha.isSelected()) {
            linha.setStyle("");
            return;
        }

        if (conta.isVencida()) {
            linha.setStyle("-fx-background-color: #fdecec;");
            return;
        }

        linha.setStyle("");
    }

    /**
     * Configura a formatação monetária da coluna Valor.
     */
    private void configurarFormatacaoColunaValor() {

        colValor.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean empty) {
                super.updateItem(valor, empty);

                if (empty || valor == null) {
                    setText(null);
                    return;
                }

                setText(formatarValor(valor));
            }
        });
    }

    /**
     * Configura a formatação da coluna Vencimento.
     */
    private void configurarFormatacaoColunaVencimento() {

        colVencimento.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate dataVencimento, boolean empty) {
                super.updateItem(dataVencimento, empty);

                if (empty || dataVencimento == null) {
                    setText(null);
                    return;
                }

                setText(formatarData(dataVencimento));
            }
        });
    }

    /**
     * Configura o listener de seleção da tabela.
     *
     * Quando uma conta é selecionada, preenche o painel lateral
     * e habilita o botão Receber Conta.
     */
    private void configurarSelecaoTabela() {

        tabelaContasPendentes
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, contaAnterior, contaAtual) -> {
                    if (contaAtual == null) {
                        limparPainelContaSelecionada();
                        return;
                    }

                    preencherPainelContaSelecionada(contaAtual);
                    btnReceberConta.setDisable(false);
                });
    }

    /**
     * Carrega as contas pendentes usando o Service.
     *
     * O Controller não acessa DAO e não abre Connection diretamente.
     */
    private void carregarContasPendentes() {

        try {
            tabelaContasPendentes.getSelectionModel().clearSelection();
            limparPainelContaSelecionada();

            List<ContaReceberListagemView> listaRetornada =
                    contaReceberService.listarContasPendentes();

            if (listaRetornada == null) {
                contasPendentes.clear();
            } else {
                contasPendentes.setAll(listaRetornada);
            }

            tabelaContasPendentes.getSelectionModel().clearSelection();
            limparPainelContaSelecionada();
            atualizarContador();

        } catch (RuntimeException e) {
            contasPendentes.clear();
            tabelaContasPendentes.getSelectionModel().clearSelection();
            limparPainelContaSelecionada();
            atualizarContador();

            System.err.println("[ERRO] Falha ao carregar contas pendentes.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar as contas pendentes."
            );
        }
    }

    /**
     * Preenche o painel lateral com os dados da conta selecionada.
     */
    private void preencherPainelContaSelecionada(
            ContaReceberListagemView contaSelecionada
    ) {

        if (contaSelecionada == null) {
            limparPainelContaSelecionada();
            return;
        }

        lblContaId.setText(formatarId(contaSelecionada.getContaReceberId()));
        lblCliente.setText(formatarTexto(contaSelecionada.getNomeCliente()));
        lblVendaId.setText(formatarId(contaSelecionada.getVendaId()));
        lblValor.setText(formatarValor(contaSelecionada.getValor()));
        lblVencimento.setText(formatarData(contaSelecionada.getDataVencimento()));
        lblStatus.setText(formatarStatus(contaSelecionada.getStatus()));
        lblSituacao.setText(formatarSituacao(contaSelecionada));
    }

    /**
     * Limpa o painel lateral e desabilita o botão Receber Conta.
     */
    private void limparPainelContaSelecionada() {
        lblContaId.setText("—");
        lblCliente.setText("—");
        lblVendaId.setText("—");
        lblValor.setText("R$ 0,00");
        lblVencimento.setText("—");
        lblStatus.setText("—");
        lblSituacao.setText("—");

        btnReceberConta.setDisable(true);
    }

    /**
     * Formata IDs para exibição segura.
     */
    private String formatarId(Integer id) {

        if (id == null || id <= 0) {
            return "—";
        }

        return id.toString();
    }

    /**
     * Formata texto para exibição segura.
     */
    private String formatarTexto(String texto) {

        if (texto == null || texto.isBlank()) {
            return "—";
        }

        return texto.trim();
    }

    /**
     * Formata valores monetários no padrão brasileiro.
     */
    private String formatarValor(BigDecimal valor) {

        BigDecimal valorSeguro = valor != null ? valor : BigDecimal.ZERO;

        NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );

        return formatoMoeda.format(valorSeguro).replace('\u00A0', ' ');
    }

    /**
     * Formata datas no padrão brasileiro.
     */
    private String formatarData(LocalDate data) {

        if (data == null) {
            return "—";
        }

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return data.format(formatoData);
    }

    /**
     * Formata data e hora no padrão brasileiro.
     */
    private String formatarDataHora(LocalDateTime dataHora) {

        if (dataHora == null) {
            return "—";
        }

        DateTimeFormatter formatoDataHora = DateTimeFormatter.ofPattern(
                "dd/MM/yyyy HH:mm"
        );

        return dataHora.format(formatoDataHora);
    }

    /**
     * Formata o status da conta para exibição segura.
     */
    private String formatarStatus(StatusContaReceber status) {

        if (status == null) {
            return "—";
        }

        return status.name();
    }

    /**
     * Formata a situação visual da conta selecionada.
     */
    private String formatarSituacao(ContaReceberListagemView contaSelecionada) {

        if (contaSelecionada == null) {
            return "—";
        }

        return contaSelecionada.isVencida()
                ? "Vencida"
                : "Em aberto";
    }

    /**
     * Formata a forma de pagamento para exibição amigável.
     */
    private String formatarFormaPagamento(FormaPagamento formaPagamento) {

        if (formaPagamento == null) {
            return "—";
        }

        switch (formaPagamento) {
            case DINHEIRO:
                return "Dinheiro";

            case PIX:
                return "PIX";

            case CARTAO:
                return "Cartão";

            default:
                return "—";
        }
    }

    /**
     * Atualiza o contador de contas pendentes exibidas na tabela.
     */
    private void atualizarContador() {

        int totalContasPendentes = contasPendentes.size();

        if (totalContasPendentes == 1) {
            lblTotalContas.setText("Total: 1 conta pendente");
            return;
        }

        lblTotalContas.setText(
                "Total: " + totalContasPendentes + " contas pendentes"
        );
    }

    /**
     * Ação do botão Atualizar.
     */
    @FXML
    private void onAtualizar() {
        carregarContasPendentes();
    }

    /**
     * Ação do botão Voltar.
     *
     * Retorna para a Tela Principal usando o mesmo Stage atual.
     */
    @FXML
    private void onVoltar() {

        try {
            NavegacaoUtil.abrirTela(
                    btnVoltar,
                    "/br/com/luis/view/TelaPrincipal.fxml",
                    "Tela Principal"
            );

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao voltar para a Tela Principal.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    /**
     * Ação do botão Receber Conta.
     *
     * Usa a conta selecionada, solicita a forma de pagamento,
     * confirma o recebimento integral, chama o Service e atualiza a interface.
     */
    @FXML
    private void onReceberConta() {

        ContaReceberListagemView contaSelecionada =
                tabelaContasPendentes
                        .getSelectionModel()
                        .getSelectedItem();

        if (contaSelecionada == null) {
            limparPainelContaSelecionada();

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Selecione uma conta para realizar o recebimento."
            );

            return;
        }

        Optional<FormaPagamento> formaPagamentoOptional = solicitarFormaPagamento();

        if (formaPagamentoOptional.isEmpty()) {
            return;
        }

        FormaPagamento formaPagamento = formaPagamentoOptional.get();

        boolean recebimentoConfirmado = confirmarRecebimento(
                contaSelecionada,
                formaPagamento
        );

        if (!recebimentoConfirmado) {
            return;
        }

        Integer usuarioId;

        try {
            usuarioId = obterUsuarioIdAtual();

        } catch (IllegalStateException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    e.getMessage()
            );

            return;
        }

        try {
            btnReceberConta.setDisable(true);
            btnAtualizar.setDisable(true);

            ResultadoRecebimentoConta resultado =
                    contaReceberService.receberConta(
                            contaSelecionada.getContaReceberId(),
                            formaPagamento,
                            usuarioId
                    );

            if (resultado == null) {
                throw new RuntimeException(
                        "Resultado do recebimento não retornado pelo Service."
                );
            }

            mostrarResultadoRecebimento(resultado);
            carregarContasPendentes();

        } catch (IllegalArgumentException | IllegalStateException e) {
            String mensagem = e.getMessage();

            if (mensagem == null || mensagem.isBlank()) {
                mensagem = "Não foi possível receber a conta.";
            }

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Não foi possível receber a conta",
                    mensagem
            );

        } catch (RuntimeException e) {
            System.err.println("[ERRO] Falha ao concluir recebimento da conta.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível concluir o recebimento da conta."
            );

        } finally {
            btnAtualizar.setDisable(false);

            btnReceberConta.setDisable(
                    tabelaContasPendentes
                            .getSelectionModel()
                            .getSelectedItem() == null
            );
        }
    }

    /**
     * Solicita a forma de pagamento do recebimento.
     *
     * Permite somente Dinheiro, PIX e Cartão.
     */
    private Optional<FormaPagamento> solicitarFormaPagamento() {

        Dialog<FormaPagamento> dialog = new Dialog<>();
        dialog.setTitle("Receber Conta");
        dialog.setHeaderText("Selecione a forma de pagamento.");

        ButtonType botaoConfirmar = new ButtonType(
                "Confirmar",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                botaoConfirmar,
                ButtonType.CANCEL
        );

        ComboBox<FormaPagamento> comboFormaPagamento = new ComboBox<>();
        comboFormaPagamento.getItems().addAll(
                FormaPagamento.DINHEIRO,
                FormaPagamento.PIX,
                FormaPagamento.CARTAO
        );
        comboFormaPagamento.setValue(FormaPagamento.DINHEIRO);

        comboFormaPagamento.setConverter(new StringConverter<>() {
            @Override
            public String toString(FormaPagamento formaPagamento) {
                return formatarFormaPagamento(formaPagamento);
            }

            @Override
            public FormaPagamento fromString(String texto) {
                return null;
            }
        });

        VBox conteudo = new VBox(
                8,
                new Label("Forma de pagamento:"),
                comboFormaPagamento
        );

        dialog.getDialogPane().setContent(conteudo);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == botaoConfirmar) {
                return comboFormaPagamento.getValue();
            }

            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Confirma o recebimento integral da conta selecionada.
     */
    private boolean confirmarRecebimento(
            ContaReceberListagemView contaSelecionada,
            FormaPagamento formaPagamento
    ) {

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Recebimento");
        alerta.setHeaderText(null);

        String mensagem = "Confirmar o recebimento integral da conta "
                + formatarId(contaSelecionada.getContaReceberId())
                + ",\n"
                + "do cliente "
                + formatarTexto(contaSelecionada.getNomeCliente())
                + ",\n"
                + "no valor de "
                + formatarValor(contaSelecionada.getValor())
                + ",\n"
                + "por "
                + formatarFormaPagamento(formaPagamento)
                + "?";

        alerta.setContentText(mensagem);

        Optional<ButtonType> resposta = alerta.showAndWait();

        return resposta.isPresent()
                && resposta.get() == ButtonType.OK;
    }

    /**
     * Obtém o ID real do usuário atualmente logado.
     *
     * Não usa usuário temporário nem fallback.
     */
    private Integer obterUsuarioIdAtual() {

        Usuario usuarioLogado = SessaoUsuario
                .getInstance()
                .getUsuarioLogado();

        if (usuarioLogado == null) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. Entre novamente no sistema."
            );
        }

        Integer usuarioId = usuarioLogado.getIdUsuario();

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. Entre novamente no sistema."
            );
        }

        return usuarioId;
    }

    /**
     * Mostra o resultado do recebimento após sucesso do Service.
     */
    private void mostrarResultadoRecebimento(
            ResultadoRecebimentoConta resultado
    ) {

        StringBuilder mensagem = new StringBuilder();

        mensagem.append("Recebimento realizado com sucesso.")
                .append("\n\n")
                .append("Conta: ")
                .append(formatarId(resultado.getContaReceberId()))
                .append("\n")
                .append("Valor recebido: ")
                .append(formatarValor(resultado.getValorRecebido()))
                .append("\n")
                .append("Forma de pagamento: ")
                .append(formatarFormaPagamento(resultado.getFormaPagamento()))
                .append("\n")
                .append("Data e hora: ")
                .append(formatarDataHora(resultado.getDataHoraRecebimento()))
                .append("\n")
                .append("Status: ")
                .append(formatarStatus(resultado.getStatusContaReceber()));

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Sucesso",
                mensagem.toString()
        );
    }

    /**
     * Exibe alertas padronizados para o usuário.
     */
    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensagem
    ) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
