package br.com.luis.model;

/**
 * Entidade Usuario - Representa a tabela 'Usuario' do SQLite.
 * Implementa validações (Fail-Fast) e boas práticas de segurança.
 */
public class Usuario {

    private Integer idUsuario;
    private String nome;
    private String login;
    private String senha; // Armazena o hash gerado pelo BCrypt
    private String perfil;
    private String status;

    // Construtor padrão
    public Usuario() {
    }

    // Construtor completo com validação
    public Usuario(Integer idUsuario, String nome, String login, String senhaHash, String perfil, String status) {
        this.idUsuario = idUsuario;
        setNome(nome);
        setLogin(login);
        setSenha(senhaHash);
        setPerfil(perfil);
        setStatus(status);
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório e não pode estar vazio.");
        }
        this.nome = nome.trim();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("O login é obrigatório.");
        }
        // Padroniza para evitar duplicidade (ex: Admin vs admin)
        this.login = login.trim().toLowerCase();
    }

    /**
     * A senha armazenada deve ser SEMPRE o hash gerado pelo BCrypt.
     */
    public String getSenha() { // Retorna o hash usado pelo AuthService na autenticação.
        return senha;
    }

    public void setSenha(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        this.senha = senhaHash;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            throw new IllegalArgumentException("O perfil é obrigatório.");
        }

        String perfilFormatado = perfil.trim().toUpperCase();

        if (!perfilFormatado.equals("ADMIN") && !perfilFormatado.equals("VENDEDOR")) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN ou VENDEDOR.");
        }

        this.perfil = perfilFormatado;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("O status é obrigatório.");
        }

        String statusFormatado = status.trim().toUpperCase();

        if (!statusFormatado.equals("ATIVO") && !statusFormatado.equals("INATIVO")) {
            throw new IllegalArgumentException("Status inválido. Use ATIVO ou INATIVO.");
        }

        this.status = statusFormatado;
    }

    @Override
    public String toString() {
        // Senha omitida propositalmente por segurança
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", nome='" + nome + '\'' +
                ", login='" + login + '\'' +
                ", perfil='" + perfil + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}