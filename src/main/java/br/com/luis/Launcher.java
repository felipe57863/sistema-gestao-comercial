package br.com.luis;

/**
 * Classe Wrapper para inicialização da aplicação JavaFX.
 *
 * Problema:
 * Em aplicações JavaFX (principalmente com JDK 11+ e modularização),
 * a JVM pode não conseguir iniciar corretamente uma classe que estende
 * javafx.application.Application diretamente.
 *
 * Solução:
 * Utilizar esta classe intermediária (Launcher) que NÃO estende Application.
 * Ela delega a execução para a classe Main, que contém a lógica real da aplicação.
 *
 * Benefícios:
 * - Evita erros de inicialização (ex: "JavaFX runtime components are missing")
 * - Garante compatibilidade com IDEs e execução via JAR
 * - Mantém o ponto de entrada desacoplado da UI
 */
public class Launcher {

    /**
     * Método principal da aplicação.
     * Apenas delega a execução para a classe Main (JavaFX Application).
     */
    public static void main(String[] args) {
        App.main(args);
    }
}