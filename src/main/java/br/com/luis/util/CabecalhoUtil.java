package br.com.luis.util;

import br.com.luis.model.Usuario;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Padroniza a apresentação do usuário, da data e da hora nos cabeçalhos.
 *
 * Obtém o usuário pela SessaoUsuario, mantém o relógio atualizado e interrompe
 * a animação quando a Scene da tela é substituída. Não executa regras de negócio.
 */
public final class CabecalhoUtil {

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private CabecalhoUtil() {
    }

    /**
     * Preenche o usuário da sessão e mantém a data e a hora atualizadas a cada segundo.
     *
     * O relógio é interrompido quando a Scene vinculada deixa a Window atual durante
     * uma troca de tela.
     *
     * @param lblUsuario Label que exibe o usuário da sessão.
     * @param lblDataHora Label que exibe a data e a hora atuais.
     */
    public static void configurarUsuarioEDataHora(
            Label lblUsuario,
            Label lblDataHora
    ) {
        Objects.requireNonNull(lblUsuario, "Label de usuário não pode ser nulo.");
        Objects.requireNonNull(lblDataHora, "Label de data e hora não pode ser nulo.");

        atualizarUsuario(lblUsuario);
        atualizarDataHora(lblDataHora);

        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> atualizarDataHora(lblDataHora)
                )
        );

        timeline.setCycleCount(Animation.INDEFINITE);

        registrarEncerramentoTimeline(lblDataHora, timeline);
        timeline.play();
    }

    /**
     * Atualiza o Label de usuário com os dados atuais da sessão.
     */
    private static void atualizarUsuario(Label lblUsuario) {
        Usuario usuarioLogado = SessaoUsuario.getInstance().getUsuarioLogado();
        String nomeUsuario = usuarioLogado != null ? usuarioLogado.getNome() : null;

        if (nomeUsuario == null || nomeUsuario.isBlank()) {
            lblUsuario.setText("Usuário não identificado");
            return;
        }

        lblUsuario.setText("Usuário: " + nomeUsuario.trim());
    }

    /**
     * Atualiza o Label de data e hora usando o instante local atual.
     */
    private static void atualizarDataHora(Label lblDataHora) {
        lblDataHora.setText(LocalDateTime.now().format(FORMATO_DATA_HORA));
    }

    /**
     * Registra o encerramento do Timeline quando a tela deixa a Window atual.
     */
    private static void registrarEncerramentoTimeline(
            Label lblDataHora,
            Timeline timeline
    ) {
        lblDataHora.sceneProperty().addListener(
                (observable, sceneAnterior, sceneAtual) -> {
                    if (sceneAtual != null) {
                        acompanharWindow(sceneAtual, timeline);
                    }
                }
        );

        Scene sceneAtual = lblDataHora.getScene();

        if (sceneAtual != null) {
            acompanharWindow(sceneAtual, timeline);
        }
    }

    /**
     * Para o Timeline quando uma Scene anteriormente vinculada perde sua Window.
     */
    private static void acompanharWindow(Scene scene, Timeline timeline) {
        scene.windowProperty().addListener(
                (observable, janelaAnterior, janelaAtual) -> {
                    if (janelaAnterior != null && janelaAtual == null) {
                        timeline.stop();
                    }
                }
        );
    }
}
