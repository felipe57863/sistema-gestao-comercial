package br.com.luis.service;

import br.com.luis.dao.PromocaoDAO;
import br.com.luis.model.Promocao;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Camada de Serviço (Regras de Negócio) para Promoções.
 * Garante a integridade dos dados e orquestra transações complexas.
 */
public class PromocaoService {

    private final PromocaoDAO promocaoDAO;

    public PromocaoService() {
        // Inicializa o DAO que será orquestrado por este Service
        this.promocaoDAO = new PromocaoDAO();
    }

    /**
     * RN22: Cadastra uma nova promoção e inativa as antigas ativas do mesmo produto.
     * Operação Transacional (Tudo ou Nada).
     */
    public void cadastrarPromocaoNova(Promocao promocao) {

        // 1. Fail-Fast (Defesa antes de tocar no banco)
        if (promocao == null || promocao.getProduto() == null || promocao.getProduto().getIdProduto() == null) {
            throw new IllegalArgumentException("Dados da promoção ou produto inválidos para cadastro.");
        }

        Connection conn = null;

        try {
            // 2. Abre a conexão e assume o controle manual da transação
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false); // Bloqueia o salvamento automático do SQLite

            // 3. Executa as operações em cadeia no DAO passando a MESMA conexão
            promocaoDAO.inativarPromocoesAnteriores(conn, promocao.getProduto().getIdProduto());
            promocaoDAO.cadastrar(conn, promocao);

            // 4. Se chegou até aqui sem erros, confirma tudo no banco!
            conn.commit();

        } catch (Exception e) {
            // 5. Deu erro em qualquer etapa? Desfaz TUDO!
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException("Erro crítico: Falha ao tentar realizar o rollback da transação.", ex);
                }
            }
            throw new RuntimeException("Erro ao aplicar a promoção. A operação foi cancelada: " + e.getMessage(), e);

        } finally {
            // 6. Limpeza obrigatória (Libera o banco para o resto do sistema)
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Devolve para o padrão
                    conn.close();
                } catch (SQLException e) {
                    // Apenas loga no console, pois a transação principal já foi resolvida
                    e.printStackTrace();
                }
            }
        }
    }
}