package br.com.luis.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    private NavegacaoUtil() {
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
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
    }
}
