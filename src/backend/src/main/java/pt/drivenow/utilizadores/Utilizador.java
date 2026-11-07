package pt.drivenow.utilizadores;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "utilizador")
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    private String nif;

    private String telefone;

    /** Id do Business Partner no SAP (preenchido pelo SapClient). */
    private String sapBusinessPartnerId;

    /** Id do cliente no Moloni (preenchido pelo MoloniClient). */
    private Long moloniClienteId;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    protected Utilizador() {
    }

    public Utilizador(String nome, String email, String nif, String telefone) {
        this.nome = nome;
        this.email = email;
        this.nif = nif;
        this.telefone = telefone;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getNif() { return nif; }
    public String getTelefone() { return telefone; }
    public String getSapBusinessPartnerId() { return sapBusinessPartnerId; }
    public Long getMoloniClienteId() { return moloniClienteId; }
    public Instant getCriadoEm() { return criadoEm; }

    public void setSapBusinessPartnerId(String sapBusinessPartnerId) { this.sapBusinessPartnerId = sapBusinessPartnerId; }
    public void setMoloniClienteId(Long moloniClienteId) { this.moloniClienteId = moloniClienteId; }
}
