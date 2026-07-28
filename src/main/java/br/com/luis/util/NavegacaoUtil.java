package br.com.luis.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

/**
 * Utilitário responsável pelo carregamento de telas FXML no Stage atual.
 *
 * Não mantém estado e não trata regras de negócio ou mensagens visuais.
 */
public final class NavegacaoUtil {

    private static final double LARGURA_RESTAURADA_PREFERENCIAL = 1200.0;
    private static final double ALTURA_RESTAURADA_PREFERENCIAL = 700.0;
    private static final double LARGURA_MINIMA = 1000.0;
    private static final double ALTURA_MINIMA = 650.0;

    private NavegacaoUtil() {
    }

    /**
     * Configura a nova Scene de uma tela funcional no Stage atual.
     *
     * Identifica o monitor atual antes de retirar a maximização, aplica o padrão
     * de tamanho restaurado limitado à área visual disponível, configura os
     * tamanhos mínimos efetivos, centraliza a janela e reaplica a maximização no
     * próximo ciclo da JavaFX Application Thread.
     *
     * @param stage Stage atual da aplicação.
     * @param novaScene Scene da tela funcional que será exibida.
     * @throws IllegalArgumentException se o Stage ou a Scene forem nulos.
     */
    public static void configurarTelaFuncional(
            Stage stage,
            Scene novaScene
    ) {

        if (stage == null) {
            throw new IllegalArgumentException("Stage não pode ser nulo.");
        }

        if (novaScene == null) {
            throw new IllegalArgumentException("Scene não pode ser nula.");
        }

        Screen monitorAtual = identificarMonitorAtual(stage);
        Rectangle2D areaVisual = monitorAtual.getVisualBounds();

        double larguraRestaurada = Math.min(
                LARGURA_RESTAURADA_PREFERENCIAL,
                areaVisual.getWidth()
        );
        double alturaRestaurada = Math.min(
                ALTURA_RESTAURADA_PREFERENCIAL,
                areaVisual.getHeight()
        );
        double larguraMinimaEfetiva = Math.min(
                LARGURA_MINIMA,
                areaVisual.getWidth()
        );
        double alturaMinimaEfetiva = Math.min(
                ALTURA_MINIMA,
                areaVisual.getHeight()
        );

        stage.setMaximized(false);
        stage.setScene(novaScene);
        stage.setWidth(larguraRestaurada);
        stage.setHeight(alturaRestaurada);
        stage.setMinWidth(larguraMinimaEfetiva);
        stage.setMinHeight(alturaMinimaEfetiva);
        stage.setX(
                areaVisual.getMinX()
                        + (areaVisual.getWidth() - larguraRestaurada) / 2.0
        );
        stage.setY(
                areaVisual.getMinY()
                        + (areaVisual.getHeight() - alturaRestaurada) / 2.0
        );

        Platform.runLater(() -> stage.setMaximized(true));
    }

    /**
     * Identifica o monitor que contém o centro do Stage atual.
     *
     * @param stage Stage usado como referência de posicionamento.
     * @return monitor atual ou o monitor principal quando não for identificado.
     */
    private static Screen identificarMonitorAtual(Stage stage) {

        double centroX = stage.getX() + stage.getWidth() / 2.0;
        double centroY = stage.getY() + stage.getHeight() / 2.0;

        return Screen.getScreensForRectangle(
                        centroX,
                        centroY,
                        1.0,
                        1.0
                )
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary());
    }

    /**
     * Carrega um FXML e substitui a Scene do Stage associado ao componente de origem.
     *
     * @param componenteOrigem componente pertencente à Scene atual.
     * @param caminhoFxml caminho absoluto do FXML no classpath.
     * @param tituloModulo título do módulo aberto.
     * @throws IOException se ocorrer falha durante o carregamento do FXML.
     * @throws IllegalArgumentException se algum argumento for inválido.
     * @throws IllegalStateException se o recurso, a Scene, a Window ou o Stage não estiver disponível.
     */
    public static void abrirTela(
            Node componenteOrigem,
            String caminhoFxml,
            String tituloModulo
    ) throws IOException {

        if (componenteOrigem == null) {
            throw new IllegalArgumentException("Componente de origem não pode ser nulo.");
        }

        if (caminhoFxml == null || caminhoFxml.isBlank()) {
            throw new IllegalArgumentException("Caminho do FXML não pode ser nulo ou vazio.");
        }

        if (tituloModulo == null || tituloModulo.isBlank()) {
            throw new IllegalArgumentException("Título do módulo não pode ser nulo ou vazio.");
        }

        URL fxmlLocation = NavegacaoUtil.class.getResource(caminhoFxml);

        if (fxmlLocation == null) {
            throw new IllegalStateException("FXML não encontrado: " + caminhoFxml);
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene sceneAtual = componenteOrigem.getScene();

        if (sceneAtual == null) {
            throw new IllegalStateException("Scene atual não encontrada para o componente de origem.");
        }

        Window windowAtual = sceneAtual.getWindow();

        if (windowAtual == null) {
            throw new IllegalStateException("Window atual não encontrada para o componente de origem.");
        }

        if (!(windowAtual instanceof Stage stage)) {
            throw new IllegalStateException("A Window atual não é um Stage válido.");
        }

        stage.setTitle("ERP Comercial - " + tituloModulo);
        configurarTelaFuncional(stage, new Scene(root));
    }
}
