package com.example.aep;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "movimentacao")
@SQLRestriction("tipo_movimentacao = 'doação'")
public class Doacao implements Destinacao{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote possui;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
    @Column(name = "tipo_movimentacao", nullable = false, length = 20, updatable = false)
    private String tipo = "doação";
    @Column(nullable = false)
    private Integer quantidade;
    @Column(name = "data_ocorrencia", nullable = false)
    private LocalDate date;
    @Column(length = 200)
    private String entidades;


    protected Doacao(){}
    public Doacao(Lote possui, Integer quantidade, LocalDate date, String entidades, Usuario usuario) {
        if(possui == null)
            throw new IllegalArgumentException("Descarte incoreto ou invalido");
        if(quantidade == null || quantidade<0)
            throw new IllegalArgumentException("Quantidade invalida");
        if(entidades == null || entidades.isBlank())
            throw new IllegalArgumentException("justificativa invalida");
        if(usuario == null)
            throw new IllegalArgumentException("Operador invalido");

        this.possui = possui;
        possui.estoqueBaixa(quantidade);
        this.quantidade = quantidade;
        this.tipo = "doação";
        this.date = date;
        this.entidades = entidades;
        this.usuario = usuario;

    }

    @Override
    public double caulcularImpaquito() {
        return possui.getPreco().doubleValue() * quantidade;
    }


    public Integer getId() {
        return id;
    }

    public Lote getPossui() {
        return possui;
    }

    public String getTipo() {
        return tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getEntidades() {
        return entidades;
    }


}
