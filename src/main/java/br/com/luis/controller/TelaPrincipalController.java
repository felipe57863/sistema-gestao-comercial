package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.DashboardService;
import br.com.luis.service.DashboardService.PeriodoDashboard;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.DashboardResumoView;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller da Tela Principal apresentada após o login.
 *
 * Coordena a exibição dos indicadores do dashboard, a seleção do período,
 * o carregamento assíncrono dos dados, a navegação para os módulos funcionais
 * e o encerramento seguro da sessão.
 *
 * O carregamento do dashboard é executado fora do JavaFX Application Thread
 * por meio de uma única Task. O Controller mantém somente a Task considerada
 * atual, invalida carregamentos anteriores e impede que uma Task antiga altere
 * a interface depois de uma atualização mais recente ou da saída da tela.
 *
 * Os últimos dados válidos permanecem visíveis durante novas consultas e
 * também quando ocorre uma falha. O logout cancela o carregamento atual,
 * encerra a SessaoUsuario e retorna ao Login reutilizando o mesmo Stage.
 *
 * O Controller não executa SQL nem contém regras de negócio, delegando
 * a consolidação dos indicadores ao {@link DashboardService}.
 */
public class TelaPrincipalController {

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MENSAGEM_ACESSO_NEGADO_RELATORIO =
            "Usuário não autorizado a consultar o relatório financeiro.";

    private final DashboardService dashboardService;
    private final NumberFormat formatadorMoeda;

    private Task<DashboardResumoView> tarefaDashboardAtual;

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnClientes;
    @FXML private Button btnProdutos;
    @FXML private Button btnPrazosPagamento;
    @FXML private Button btnRelatorios;
    @FXML private Button btnUsuarios;

    @FXML private ComboBox<PeriodoDashboard> cmbPeriodoDashboard;
    @FXML private Button btnAtualizarDashboard;
    @FXML private ProgressIndicator progressoDashboard;

    @FXML private Label lblQuantidadeVendas;
    @FXML private Label lblValorTotalVendido;
    @FXML private Label lblPeriodoConsultado;
    @FXML private Label lblValorRecebidoLiquido;

    @FXML private Label lblQuantidadeContasPendentes;
    @FXML private Label lblValorTotalPendente;
    @FXML private Label lblQuantidadeContasVencidas;
    @FXML private Label lblValorTotalVencido;

    @FXML private Label lblQuantidadeProdutosEstoqueBaixo;

    /**
     * Cria o Controller com uma única instância do serviço responsável pelo
     * dashboard e configura o formatador monetário brasileiro.
     *
     * O construtor público sem argumentos mantém a instanciação compatível
     * com o FXMLLoader.
     */
    public TelaPrincipalController() {
        this.dashboardService = new DashboardService();

        this.formatadorMoeda =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("pt-BR")
                );
    }

    /**
     * Inicializa o cabeçalho, as opções fixas de período, a ação de atualização
     * e o primeiro carregamento do dashboard.
     *
     * A alteração isolada da seleção do ComboBox não executa uma consulta.
     * O carregamento posterior ocorre somente pelo botão Atualizar.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );

        configurarVisibilidadeBotaoRelatorios();
        configurarVisibilidadeOpcoesAdministrativas();

        cmbPeriodoDashboard
                .getItems()
                .setAll(PeriodoDashboard.values());

        cmbPeriodoDashboard
                .getSelectionModel()
                .select(PeriodoDashboard.MES_ATUAL);

        btnAtualizarDashboard.setOnAction(
                event -> iniciarCarregamentoDashboard()
        );

        configurarEstadoCarregamentoDashboard(false);

        iniciarCarregamentoDashboard();
    }

    /**
     * Configura a visibilidade do botão de relatórios conforme o usuário
     * mantido na sessão atual.
     *
     * Essa proteção é apenas visual. A autorização definitiva continua sendo
     * realizada pelo Service do relatório financeiro.
     */
    private void configurarVisibilidadeBotaoRelatorios() {

        boolean administrador =
                usuarioAtualEhAdministrador();

        btnRelatorios.setVisible(administrador);
        btnRelatorios.setManaged(administrador);
        btnRelatorios.setDisable(!administrador);
    }

    /**
     * Limita visualmente Produtos, Clientes, Prazos de Pagamento e Usuários ao
     * administrador apto.
     * As opções também são protegidas pelos respectivos métodos de abertura, e
     * as operações de usuários permanecem autorizadas pelo Service.
     */
    private void configurarVisibilidadeOpcoesAdministrativas() {
        boolean administrador = usuarioAtualEhAdministrador();

        btnProdutos.setVisible(administrador);
        btnProdutos.setManaged(administrador);
        btnProdutos.setDisable(!administrador);

        btnClientes.setVisible(administrador);
        btnClientes.setManaged(administrador);
        btnClientes.setDisable(!administrador);

        btnPrazosPagamento.setVisible(administrador);
        btnPrazosPagamento.setManaged(administrador);
        btnPrazosPagamento.setDisable(!administrador);

        btnUsuarios.setVisible(administrador);
        btnUsuarios.setManaged(administrador);
        btnUsuarios.setDisable(!administrador);
    }

    /**
     * Verifica se a sessão atual possui um administrador com ID válido.
     *
     * @return true quando existe administrador ativo e liberado para o acesso.
     */
    private boolean usuarioAtualEhAdministrador() {

        Usuario usuarioLogado =
                SessaoUsuario
                        .getInstance()
                        .getUsuarioLogado();

        if (usuarioLogado == null) {
            return false;
        }

        Integer usuarioId =
                usuarioLogado.getIdUsuario();

        return usuarioId != null
                && usuarioId > 0
                && "ADMIN".equals(usuarioLogado.getPerfil())
                && "ATIVO".equals(usuarioLogado.getStatus())
                && !usuarioLogado.isTrocaSenhaObrigatoria();
    }

    /**
     * Inicia o carregamento completo dos indicadores para o período atualmente
     * selecionado.
     *
     * Uma seleção inválida não interrompe uma Task que já esteja em execução.
     * Depois da validação do período, qualquer carregamento anterior é
     * invalidado antes da criação da nova Task.
     */
    private void iniciarCarregamentoDashboard() {
        PeriodoDashboard periodoSelecionado =
                cmbPeriodoDashboard.getValue();

        if (periodoSelecionado == null) {
            if (tarefaDashboardAtual == null) {
                configurarEstadoCarregamentoDashboard(false);
            }

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Selecione um período para atualizar o dashboard."
            );

            return;
        }

        cancelarCarregamentoDashboard();

        Task<DashboardResumoView> novaTarefa =
                criarTarefaDashboard(periodoSelecionado);

        tarefaDashboardAtual = novaTarefa;

        configurarHandlersDashboard(novaTarefa);
        configurarEstadoCarregamentoDashboard(true);

        Thread thread = new Thread(
                novaTarefa,
                "dashboard-carregamento"
        );

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Cria a Task responsável por solicitar uma única fotografia completa
     * dos indicadores ao DashboardService.
     *
     * Nenhum componente JavaFX ou formatador visual é acessado durante
     * a execução em segundo plano.
     *
     * @param periodoSelecionado período que será aplicado pelo serviço.
     * @return Task preparada para carregar o resumo do dashboard.
     */
    private Task<DashboardResumoView> criarTarefaDashboard(
            PeriodoDashboard periodoSelecionado
    ) {

        return new Task<>() {

            @Override
            protected DashboardResumoView call() {
                return dashboardService.carregarResumo(
                        periodoSelecionado
                );
            }
        };
    }

    /**
     * Configura os handlers da Task usando proteção por identidade.
     *
     * Cada handler somente pode modificar a interface quando sua Task ainda
     * for exatamente a instância armazenada em tarefaDashboardAtual.
     *
     * @param tarefa Task cujos eventos serão tratados.
     */
    private void configurarHandlersDashboard(
            Task<DashboardResumoView> tarefa
    ) {

        tarefa.setOnSucceeded(event -> {
            if (tarefaDashboardAtual != tarefa) {
                return;
            }

            DashboardResumoView resumo =
                    tarefa.getValue();

            if (resumo == null) {
                tratarFalhaCarregamentoDashboard(
                        tarefa,
                        new IllegalStateException(
                                "O carregamento do dashboard "
                                        + "não retornou um resumo."
                        )
                );

                return;
            }

            try {
                atualizarIndicadoresDashboard(resumo);

            } catch (RuntimeException e) {
                tratarFalhaCarregamentoDashboard(
                        tarefa,
                        e
                );

                return;
            }

            finalizarCarregamentoDashboard(tarefa);
        });

        tarefa.setOnFailed(event -> {
            if (tarefaDashboardAtual != tarefa) {
                return;
            }

            tratarFalhaCarregamentoDashboard(
                    tarefa,
                    tarefa.getException()
            );
        });

        tarefa.setOnCancelled(event -> {
            if (tarefaDashboardAtual != tarefa) {
                return;
            }

            finalizarCarregamentoDashboard(tarefa);
        });
    }

    /**
     * Define o estado visual dos controles durante ou após um carregamento.
     *
     * Os valores já apresentados nos cartões não são alterados.
     *
     * @param carregando true para indicar consulta em andamento; false para
     *                   restaurar os controles.
     */
    private void configurarEstadoCarregamentoDashboard(
            boolean carregando
    ) {

        cmbPeriodoDashboard.setDisable(carregando);
        btnAtualizarDashboard.setDisable(carregando);

        progressoDashboard.setVisible(carregando);
        progressoDashboard.setManaged(carregando);
    }

    /**
     * Finaliza visualmente uma Task somente quando ela ainda for a Task atual.
     *
     * @param tarefa Task que solicitou a finalização.
     */
    private void finalizarCarregamentoDashboard(
            Task<DashboardResumoView> tarefa
    ) {

        if (tarefaDashboardAtual != tarefa) {
            return;
        }

        tarefaDashboardAtual = null;

        configurarEstadoCarregamentoDashboard(false);
    }

    /**
     * Cancela e invalida o carregamento atual sem apagar os indicadores e sem
     * apresentar alerta.
     *
     * A referência é limpa antes do cancelamento para impedir que os handlers
     * da Task antiga alterem posteriormente a interface.
     */
    private void cancelarCarregamentoDashboard() {
        Task<DashboardResumoView> tarefaAnterior =
                tarefaDashboardAtual;

        tarefaDashboardAtual = null;

        if (tarefaAnterior != null
                && !tarefaAnterior.isDone()) {

            tarefaAnterior.cancel(true);
        }

        configurarEstadoCarregamentoDashboard(false);
    }

    /**
     * Trata uma falha pertencente à Task atual.
     *
     * Os valores já exibidos permanecem intactos. A causa técnica é registrada
     * somente no console, enquanto o usuário recebe uma mensagem amigável.
     *
     * @param tarefa Task que apresentou a falha.
     * @param causa causa original ou null quando não foi informada.
     */
    private void tratarFalhaCarregamentoDashboard(
            Task<DashboardResumoView> tarefa,
            Throwable causa
    ) {

        if (tarefaDashboardAtual != tarefa) {
            return;
        }

        Throwable causaEfetiva =
                causa != null
                        ? causa
                        : new IllegalStateException(
                        "A falha do carregamento do dashboard "
                                + "não informou uma causa."
                );

        System.err.println(
                "[ERRO] Falha ao carregar os indicadores "
                        + "do dashboard."
        );

        causaEfetiva.printStackTrace();

        finalizarCarregamentoDashboard(tarefa);

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível atualizar os indicadores "
                        + "do dashboard.\n"
                        + "Os dados já exibidos foram mantidos."
        );
    }

    /**
     * Prepara todos os textos da fotografia recebida e somente depois os aplica
     * aos componentes visuais.
     *
     * Este método não consulta banco, não recalcula indicadores e não modifica
     * o DashboardResumoView.
     *
     * @param resumo fotografia completa dos indicadores.
     */
    private void atualizarIndicadoresDashboard(
            DashboardResumoView resumo
    ) {

        if (resumo == null) {
            throw new IllegalArgumentException(
                    "O resumo do dashboard não pode ser nulo."
            );
        }

        String textoQuantidadeVendas =
                formatarQuantidadeVendas(
                        resumo.getQuantidadeVendas()
                );

        String textoValorTotalVendido =
                formatarMoeda(
                        resumo.getValorTotalVendido()
                );

        String textoPeriodoConsultado =
                formatarPeriodoConsultado(
                        resumo.getDataInicial(),
                        resumo.getDataFinal()
                );

        String textoValorRecebidoLiquido =
                formatarMoeda(
                        resumo.getValorRecebidoLiquido()
                );

        String textoQuantidadeContasPendentes =
                formatarQuantidadeContas(
                        resumo.getQuantidadeContasPendentes()
                );

        String textoValorTotalPendente =
                formatarMoeda(
                        resumo.getValorTotalPendente()
                );

        String textoQuantidadeContasVencidas =
                formatarQuantidadeContas(
                        resumo.getQuantidadeContasVencidas()
                );

        String textoValorTotalVencido =
                formatarMoeda(
                        resumo.getValorTotalVencido()
                );

        String textoQuantidadeProdutosEstoqueBaixo =
                formatarQuantidadeProdutos(
                        resumo.getQuantidadeProdutosEstoqueBaixo()
                );

        lblQuantidadeVendas.setText(
                textoQuantidadeVendas
        );

        lblValorTotalVendido.setText(
                textoValorTotalVendido
        );

        lblPeriodoConsultado.setText(
                textoPeriodoConsultado
        );

        lblValorRecebidoLiquido.setText(
                textoValorRecebidoLiquido
        );

        lblQuantidadeContasPendentes.setText(
                textoQuantidadeContasPendentes
        );

        lblValorTotalPendente.setText(
                textoValorTotalPendente
        );

        lblQuantidadeContasVencidas.setText(
                textoQuantidadeContasVencidas
        );

        lblValorTotalVencido.setText(
                textoValorTotalVencido
        );

        lblQuantidadeProdutosEstoqueBaixo.setText(
                textoQuantidadeProdutosEstoqueBaixo
        );
    }

    /**
     * Formata um valor monetário usando o padrão brasileiro.
     *
     * O método é chamado somente no JavaFX Application Thread.
     *
     * @param valor valor monetário que será apresentado.
     * @return valor formatado em moeda brasileira.
     */
    private String formatarMoeda(
            BigDecimal valor
    ) {

        if (valor == null) {
            throw new IllegalArgumentException(
                    "O valor monetário não pode ser nulo."
            );
        }

        return formatadorMoeda.format(valor);
    }

    /**
     * Formata as datas inclusivas efetivamente devolvidas pelo resumo.
     *
     * @param dataInicial data inicial inclusiva.
     * @param dataFinal data final inclusiva.
     * @return texto do período apresentado no cartão de vendas.
     */
    private String formatarPeriodoConsultado(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {

        if (dataInicial == null) {
            throw new IllegalArgumentException(
                    "A data inicial do dashboard não pode ser nula."
            );
        }

        if (dataFinal == null) {
            throw new IllegalArgumentException(
                    "A data final do dashboard não pode ser nula."
            );
        }

        String dataInicialFormatada =
                dataInicial.format(FORMATADOR_DATA);

        if (dataInicial.equals(dataFinal)) {
            return "Período: "
                    + dataInicialFormatada;
        }

        String dataFinalFormatada =
                dataFinal.format(FORMATADOR_DATA);

        return "Período: "
                + dataInicialFormatada
                + " a "
                + dataFinalFormatada;
    }

    /**
     * Formata a quantidade de vendas com singular ou plural.
     */
    private String formatarQuantidadeVendas(
            int quantidade
    ) {

        return quantidade
                + (quantidade == 1
                ? " venda"
                : " vendas");
    }

    /**
     * Formata a quantidade de contas com singular ou plural.
     */
    private String formatarQuantidadeContas(
            int quantidade
    ) {

        return quantidade
                + (quantidade == 1
                ? " conta"
                : " contas");
    }

    /**
     * Formata a quantidade de produtos com singular ou plural.
     */
    private String formatarQuantidadeProdutos(
            int quantidade
    ) {

        return quantidade
                + (quantidade == 1
                ? " produto"
                : " produtos");
    }

    /**
     * Abre a tela funcional de cadastro de clientes no mesmo Stage.
     */
    @FXML
    public void abrirClientes() {
        if (!usuarioAtualEhAdministrador()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    "O cadastro de clientes é exclusivo para administradores ativos."
            );
            return;
        }

        abrirTela(
                "/br/com/luis/view/Cliente.fxml",
                "Cadastro de Clientes"
        );
    }

    /**
     * Abre a tela funcional de cadastro de produtos no mesmo Stage.
     */
    @FXML
    public void abrirProdutos() {
        if (!usuarioAtualEhAdministrador()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    "O cadastro de produtos é exclusivo para administradores ativos."
            );
            return;
        }

        abrirTela(
                "/br/com/luis/view/Produto.fxml",
                "Cadastro de Produtos"
        );
    }

    /**
     * Abre a tela administrativa de prazos de pagamento no mesmo Stage.
     */
    @FXML
    public void abrirPrazosPagamento() {
        if (!usuarioAtualEhAdministrador()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    "O gerenciamento de prazos de pagamento é exclusivo para administradores ativos."
            );
            return;
        }

        abrirTela(
                "/br/com/luis/view/PrazoPagamento.fxml",
                "Prazos de Pagamento"
        );
    }

    /**
     * Abre a tela de registro de venda.
     *
     * A tela de destino mantém o carrinho e realiza os fluxos de venda
     * à vista e a prazo, delegando suas regras de negócio e persistência
     * aos Services.
     */
    @FXML
    public void abrirVendas() {
        abrirTela(
                "/br/com/luis/view/RegistroVenda.fxml",
                "Registro de Venda"
        );
    }

    /**
     * Abre a tela funcional de Contas a Receber no mesmo Stage.
     */
    @FXML
    public void abrirContasReceber() {
        abrirTela(
                "/br/com/luis/view/ContasReceber.fxml",
                "Contas a Receber"
        );
    }

    /**
     * Abre a tela funcional de Histórico de Vendas no mesmo Stage.
     */
    @FXML
    public void abrirHistoricoVendas() {
        abrirTela(
                "/br/com/luis/view/HistoricoVendas.fxml",
                "Histórico de Vendas"
        );
    }

    /**
     * Apresenta as opções de relatório efetivamente implementadas.
     *
     * O carregamento do dashboard somente é cancelado quando o usuário
     * confirma a abertura de uma opção. O cancelamento do diálogo mantém
     * integralmente a Tela Principal.
     */
    @FXML
    public void abrirRelatorios() {

        if (!usuarioAtualEhAdministrador()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    MENSAGEM_ACESSO_NEGADO_RELATORIO
            );

            return;
        }

        OpcaoRelatorio opcaoInicial =
                OpcaoRelatorio.MOVIMENTACOES_FINANCEIRAS;

        ChoiceDialog<OpcaoRelatorio> dialog =
                new ChoiceDialog<>(
                        opcaoInicial,
                        OpcaoRelatorio.values()
                );

        dialog.setTitle("Relatórios");
        dialog.setHeaderText(
                "Selecione o relatório que deseja consultar."
        );
        dialog.setContentText("Relatório:");

        Optional<OpcaoRelatorio> escolha =
                dialog.showAndWait();

        if (escolha.isEmpty()) {
            return;
        }

        OpcaoRelatorio opcaoSelecionada =
                escolha.get();

        abrirTela(
                opcaoSelecionada.getCaminhoFxml(),
                opcaoSelecionada.getTituloTela()
        );
    }

    /**
     * Abre a Gestão de Usuários como tela funcional no mesmo Stage.
     */
    @FXML
    public void abrirGestaoUsuarios() {
        if (!usuarioAtualEhAdministrador()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    "A gestão de usuários é exclusiva para administradores ativos."
            );
            return;
        }

        abrirTela(
                "/br/com/luis/view/GestaoUsuarios.fxml",
                "Gestão de Usuários"
        );
    }

    /**
     * Cancela qualquer carregamento pendente do dashboard, encerra a sessão
     * atual e retorna à tela de Login reutilizando o mesmo Stage.
     *
     * A aplicação permanece aberta no fluxo normal. O Login é preparado antes
     * do encerramento da sessão para evitar que uma falha de carregamento deixe
     * a Tela Principal ativa sem um usuário autenticado.
     */
    @FXML
    public void sair() {
        cancelarCarregamentoDashboard();
        retornarParaLogin();
    }

    /**
     * Prepara completamente a tela de Login antes de encerrar a sessão atual.
     *
     * Falhas ocorridas durante a localização ou o carregamento do FXML mantêm
     * a sessão e a Tela Principal. Depois que a sessão é encerrada, qualquer
     * falha ao aplicar o Login provoca o fechamento do Stage por segurança.
     */
    private void retornarParaLogin() {
        Scene cenaLogin;
        Stage stage;

        try {
            URL fxmlLocation =
                    getClass().getResource(
                            "/br/com/luis/view/Login.fxml"
                    );

            if (fxmlLocation == null) {
                throw new IllegalStateException(
                        "Login.fxml não encontrado."
                );
            }

            FXMLLoader loader =
                    new FXMLLoader(fxmlLocation);

            Parent root = loader.load();

            cenaLogin = new Scene(root);
            stage = obterStageAtual();

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao preparar a tela de Login. "
                            + "A sessão atual foi mantida."
            );

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar à tela de Login.\n"
                            + "A sessão atual foi mantida."
            );

            return;
        }

        SessaoUsuario sessaoUsuario =
                SessaoUsuario.getInstance();

        try {
            sessaoUsuario.fazerLogout();

            if (sessaoUsuario.isUsuarioLogado()) {
                throw new IllegalStateException(
                        "A sessão permaneceu ativa após a solicitação "
                                + "de logout."
                );
            }

        } catch (RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao encerrar a sessão do usuário."
            );

            e.printStackTrace();

            if (sessaoUsuario.isUsuarioLogado()) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Erro",
                        "Não foi possível encerrar a sessão atual.\n"
                                + "A Tela Principal foi mantida."
                );

                return;
            }

            tratarFalhaAposLogout(stage, e);
            return;
        }

        try {
            stage.setMaximized(false);
            stage.setScene(cenaLogin);
            stage.setTitle("ERP Comercial - Login");
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (RuntimeException e) {
            tratarFalhaAposLogout(stage, e);
        }
    }

    /**
     * Obtém o Stage associado à Tela Principal com validações defensivas sobre
     * o Label de origem, a Scene e a Window atuais.
     *
     * @return Stage atualmente utilizado pela aplicação.
     * @throws IllegalStateException quando a estrutura visual atual não permite
     *                               localizar um Stage válido.
     */
    private Stage obterStageAtual() {
        if (lblUsuario == null) {
            throw new IllegalStateException(
                    "O componente de referência da Tela Principal "
                            + "não está disponível."
            );
        }

        Scene sceneAtual =
                lblUsuario.getScene();

        if (sceneAtual == null) {
            throw new IllegalStateException(
                    "A Scene atual da Tela Principal "
                            + "não está disponível."
            );
        }

        Window windowAtual =
                sceneAtual.getWindow();

        if (windowAtual == null) {
            throw new IllegalStateException(
                    "A Window atual da Tela Principal "
                            + "não está disponível."
            );
        }

        if (!(windowAtual instanceof Stage stage)) {
            throw new IllegalStateException(
                    "A Window atual da Tela Principal "
                            + "não é um Stage válido."
            );
        }

        return stage;
    }

    /**
     * Trata uma falha ocorrida depois que a sessão já foi encerrada.
     *
     * Como não é seguro manter a Tela Principal funcional sem usuário
     * autenticado, tenta apresentar uma mensagem amigável e fecha o Stage.
     *
     * @param stage Stage atual da aplicação.
     * @param causa falha ocorrida após o encerramento da sessão.
     */
    private void tratarFalhaAposLogout(
            Stage stage,
            RuntimeException causa
    ) {

        System.err.println(
                "[ERRO] Falha ao concluir o retorno ao Login "
                        + "após o encerramento da sessão."
        );

        causa.printStackTrace();

        try {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "A sessão foi encerrada, mas não foi possível "
                            + "abrir a tela de Login.\n"
                            + "A aplicação será fechada por segurança."
            );

        } catch (RuntimeException e) {
            System.err.println(
                    "[ERRO] Também não foi possível apresentar "
                            + "o alerta de falha do logout."
            );

            e.printStackTrace();
        }

        try {
            stage.close();

        } catch (RuntimeException e) {
            System.err.println(
                    "[ERRO] Não foi possível fechar a janela "
                            + "após a falha do logout."
            );

            e.printStackTrace();
        }
    }

    /**
     * Carrega uma tela FXML e substitui a Scene do Stage atual.
     *
     * Antes da troca, invalida qualquer carregamento pendente do dashboard.
     * Depois, atualiza o título, mantém a janela maximizada e apresenta um
     * Alert quando o recurso não pode ser localizado ou carregado.
     *
     * Este método cuida apenas da navegação e não executa regras de negócio
     * dos módulos abertos.
     */
    private void abrirTela(
            String caminhoFxml,
            String titulo
    ) {

        cancelarCarregamentoDashboard();

        try {
            NavegacaoUtil.abrirTela(
                    lblUsuario,
                    caminhoFxml,
                    titulo
            );

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir tela: "
                            + caminhoFxml
            );

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível abrir a tela solicitada."
            );
        }
    }

    /**
     * Exibe alertas padronizados.
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
    /**
     * Opções de relatório efetivamente disponíveis no sistema.
     */
    private enum OpcaoRelatorio {

        MOVIMENTACOES_FINANCEIRAS(
                "Movimentações financeiras",
                "/br/com/luis/view/RelatorioMovimentacaoFinanceira.fxml",
                "Relatório de Movimentações Financeiras"
        ),

        CONTAS_RECEBER(
                "Contas a receber",
                "/br/com/luis/view/RelatorioContaReceber.fxml",
                "Relatório de Contas a Receber"
        ),

        RELATORIOS_PRODUTOS(
                "Relatórios de produtos",
                "/br/com/luis/view/RelatorioProduto.fxml",
                "Relatório de Produtos"
        );

        private final String rotulo;
        private final String caminhoFxml;
        private final String tituloTela;

        OpcaoRelatorio(
                String rotulo,
                String caminhoFxml,
                String tituloTela
        ) {
            this.rotulo = rotulo;
            this.caminhoFxml = caminhoFxml;
            this.tituloTela = tituloTela;
        }

        private String getCaminhoFxml() {
            return caminhoFxml;
        }

        private String getTituloTela() {
            return tituloTela;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

}
