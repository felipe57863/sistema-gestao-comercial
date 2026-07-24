package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.AuthService;
import br.com.luis.util.SessaoUsuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller da Tela de Login.
 *
 * Captura login e senha da interface, delega a autenticação ao AuthService e,
 * após o sucesso, armazena o Usuario autenticado na SessaoUsuario e abre a
 * Tela Principal com o dashboard e os módulos funcionais do sistema.
 *
 * Não contém a regra interna de autenticação nem acessa DAO diretamente.
 * Também controla o feedback visual e o estado do botão durante a tentativa.
 */
public class LoginController {

    @FXML private TextField txtLogin;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;

    private AuthService authService;

    /**
     * Inicializa o AuthService e posiciona o foco no campo de login.
     */
    @FXML
    public void initialize() {
        this.authService = new AuthService();

        // Posiciona o foco inicial no campo de login.
        txtLogin.requestFocus();
    }

    /**
     * Processa a ação do botão "Entrar".
     *
     * Lê as credenciais da interface, evita múltiplos cliques durante o fluxo e
     * delega a autenticação ao AuthService. Quando autenticado, mantém o usuário
     * na SessaoUsuario, prepara a Tela Principal e exibe a confirmação ainda no
     * Login. Após o usuário fechar o alerta, substitui a Scene no mesmo Stage.
     *
     * Em falhas de autenticação ou em erros propagados durante o login,
     * apresenta a mensagem correspondente e limpa o campo de senha. Se a Tela
     * Principal não puder ser aberta após a autenticação, encerra a sessão
     * recém-criada, mantém o Login disponível e permite uma nova tentativa.
     *
     * Ao final, reabilita o botão de entrada.
     */
    @FXML
    public void fazerLogin(ActionEvent event) {

        String login = txtLogin.getText();
        String senha = txtSenha.getText();

        try {
            // Evita múltiplos cliques durante a tentativa de autenticação.
            btnEntrar.setDisable(true);

            // Delega a autenticação ao AuthService.
            Usuario usuarioAutenticado =
                    authService.autenticar(login, senha);

            // Armazena o usuário autenticado na sessão da aplicação.
            SessaoUsuario
                    .getInstance()
                    .setUsuarioLogado(usuarioAutenticado);

            // Redireciona para a Tela Principal do sistema.
            if (!abrirTelaPrincipal(usuarioAutenticado.getNome())) {
                SessaoUsuario.getInstance().fazerLogout();

                if (SessaoUsuario.getInstance().isUsuarioLogado()) {
                    throw new IllegalStateException(
                            "Não foi possível encerrar a sessão após "
                                    + "a falha de navegação."
                    );
                }

                txtSenha.clear();

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Erro",
                        "Login realizado, mas não foi possível "
                                + "abrir a tela principal."
                );

                return;
            }

        } catch (RuntimeException e) {

            // Limpa a senha após falha na autenticação ou na abertura da tela.
            txtSenha.clear();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro de Acesso",
                    e.getMessage()
            );

        } finally {
            btnEntrar.setDisable(false);
        }
    }

    /**
     * Abre a Tela Principal após a autenticação.
     *
     * Prepara a Tela Principal e valida o Stage atual antes da confirmação
     * visual. Exibe o alerta de boas-vindas enquanto o Login permanece visível
     * e, somente após o usuário fechá-lo, substitui a Scene, atualiza o título e
     * maximiza a janela. Se a preparação falhar, informa o chamador para que a
     * sessão recém-criada seja encerrada e uma nova tentativa possa ser feita.
     *
     * @param nomeUsuario nome exibido na mensagem de boas-vindas
     * @return {@code true} quando a Tela Principal foi aberta; {@code false}
     * caso contrário
     */
    private boolean abrirTelaPrincipal(String nomeUsuario) {

        try {
            URL fxmlLocation =
                    getClass().getResource(
                            "/br/com/luis/view/TelaPrincipal.fxml"
                    );

            if (fxmlLocation == null) {
                throw new IllegalStateException(
                        "TelaPrincipal.fxml não encontrado."
                );
            }

            FXMLLoader loader =
                    new FXMLLoader(fxmlLocation);

            Parent root = loader.load();

            Scene cenaPrincipal = new Scene(root);

            Scene cenaLogin = btnEntrar.getScene();

            if (cenaLogin == null
                    || !(cenaLogin.getWindow() instanceof Stage stage)) {
                throw new IllegalStateException(
                        "Stage atual da tela de Login não encontrado."
                );
            }

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Bem-vindo(a), "
                            + nomeUsuario
                            + "!"
            );

            stage.setTitle("ERP Comercial - Tela Principal");
            stage.setScene(cenaPrincipal);
            stage.setMaximized(true);

            return true;

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir TelaPrincipal.fxml."
            );

            e.printStackTrace();

            return false;
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
}
