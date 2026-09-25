package br.com.luis;

/**
 * Ponto de entrada intermediário da aplicação.
 *
 * Esta classe não estende Application e apenas delega a execução para Main.
 * O wrapper mantém separado o ponto de entrada usado para iniciar a aplicação
 * da classe que implementa o ciclo de vida do JavaFX.
 */
public class Launcher {

    /**
     * Método principal da aplicação.
     * Apenas delega a execução para a classe Main (JavaFX Application).
     */
    public static void main(String[] args) {
        Main.main(args);
    }
}