package pt.drivenow.pagamentos;

import java.time.Instant;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Meio de pagamento associado ao utilizador (um por utilizador).
 *
 * Pagamentos simulados: guarda-se só o titular, a marca, os últimos 4 dígitos e a validade.
 * O número completo do cartão, o IBAN completo e o CVV nunca são guardados.
 */
@Entity
@Table(name = "meio_pagamento")
public class MeioPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long utilizadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMeioPagamento tipo;

    @Column(nullable = false)
    private String titular;

    /** "Visa", "Mastercard", ... ou "IBAN". */
    private String marca;

    @Column(nullable = false, length = 4)
    private String ultimosDigitos;

    /** Validade do cartão (MM/AA); null nas contas bancárias. */
    private String validade;

    /** Cartão de teste que recusa sempre os pagamentos (simulação de "dívida pendente"). */
    private boolean recusaSimulada;

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected MeioPagamento() {
    }

    public MeioPagamento(Long utilizadorId) {
        this.utilizadorId = utilizadorId;
    }

    void definir(TipoMeioPagamento tipo, String titular, String marca, String ultimosDigitos, String validade,
            boolean recusaSimulada) {
        this.tipo = tipo;
        this.titular = titular;
        this.marca = marca;
        this.ultimosDigitos = ultimosDigitos;
        this.validade = validade;
        this.recusaSimulada = recusaSimulada;
        this.atualizadoEm = Instant.now();
    }

    /** "Visa •••• 4242" ou "IBAN •••• 1234". */
    public String descricao() {
        return marca + " •••• " + ultimosDigitos;
    }

    public boolean expirado() {
        if (validade == null) {
            return false;
        }
        String[] p = validade.split("/");
        YearMonth fim = YearMonth.of(2000 + Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        return YearMonth.now().isAfter(fim);
    }

    public Long getId() { return id; }
    public Long getUtilizadorId() { return utilizadorId; }
    public TipoMeioPagamento getTipo() { return tipo; }
    public String getTitular() { return titular; }
    public String getMarca() { return marca; }
    public String getUltimosDigitos() { return ultimosDigitos; }
    public String getValidade() { return validade; }
    public boolean isRecusaSimulada() { return recusaSimulada; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
