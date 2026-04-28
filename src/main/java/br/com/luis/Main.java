package br.com.luis;

import br.com.luis.util.DatabaseBuilder;

/**
 * Ponto de entrada do Sistema de Gestão Comercial (ERP).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("[INFO] Iniciando Sistema de Gestão Comercial (ERP)...");

        try {
            // Inicializa estrutura do banco
            DatabaseBuilder.buildTables();

            System.out.println("[INFO] Sistema inicializado com sucesso.");

        } catch (Exception e) {
            System.err.println("[ERRO FATAL] Falha ao iniciar o sistema.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}