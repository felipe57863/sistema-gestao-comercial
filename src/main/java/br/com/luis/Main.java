package br.com.luis;

import br.com.luis.service.AuthService;
import br.com.luis.util.DatabaseBuilder;

/**
 * Ponto de entrada do Sistema de Gestão Comercial (ERP).
 * Responsável por inicializar a aplicação e garantir o ambiente pronto.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("[INFO] Iniciando Sistema de Gestão Comercial (ERP)...");

        try {
            // 1. Inicializa a estrutura do banco (tabelas, constraints, etc.)
            DatabaseBuilder.buildTables();

            // 2. Inicializa dados essenciais (Seed)
            // Garante que exista um usuário administrador padrão
            AuthService authService = new AuthService();
            authService.inicializarAdminBase();

            // 3. Sistema pronto para uso
            System.out.println("[INFO] Sistema inicializado com sucesso.");

        } catch (Exception e) {

            // Falha crítica: sistema não pode continuar
            System.err.println("[ERROR] Falha ao iniciar o sistema: " + e.getMessage());

            // Stack trace mantido para debug (ambiente de desenvolvimento)
            e.printStackTrace();

            System.exit(1);
        }
    }
}