package com.example.aep;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;


@Entity
@Table(name = "lote")
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estabelecimento", nullable = false)
    private Estabelecimento estabelecimento;
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Operador registrarBaixa;
    @Column(name = "nome_produto", nullable = false, length = 200)
    private String nomeProduto;
    @Column(length = 200)
    private String categoria;
    @Column(nullable = false)
    private Integer quantidade;
    @Column(name = "unidade_de_medida", length = 20)
    private String unidadeDeMedida;
    @Column(precision = 19, scale = 4)
    private BigDecimal preco;
    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    protected Lote(){}

    public Lote(Estabelecimento estabelecimento, Operador registrarBaixa, String nomeProduto, String categoria, Integer quantidade, String unidadeDeMedida, BigDecimal preco, LocalDate dataValidade) {
        if (estabelecimento == null && nomeProduto == null || nomeProduto.isBlank() && quantidade== null ||quantidade<= 0 && preco == null|| preco.signum() < 0 && dataValidade == null){
            throw new IllegalArgumentException("Informações do lote estão invalidas");
        }
        else{
            this.estabelecimento = estabelecimento;
            this.registrarBaixa = registrarBaixa;
            this.nomeProduto = nomeProduto;
            this.categoria = categoria;
            this.quantidade = quantidade;
            this.unidadeDeMedida = unidadeDeMedida;
            this.preco = preco;
            this.dataValidade = dataValidade;
        }
    }

    public Integer getId() {
        return id;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public Operador getRegistrarBaixa() {
        return registrarBaixa;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public String getCategoria() {
        return categoria;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public String getUnidadeDeMedida() {
        return unidadeDeMedida;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }



    public long tempoPraVencimento() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dataValidade);
    }

    public boolean vencido(){
        boolean v = false;
        if (tempoPraVencimento() <0) {
            v = true;
        }
        return v;
    }

    public String validar(){
        long dias = tempoPraVencimento();
        if(dias <0)
            return "Não esta vencido";
        else
            return "Vencido";
    }



    public void setNomeProduto(String nomeProduto) {
        if(nomeProduto == null || nomeProduto.isBlank())
            throw new IllegalArgumentException("Nome do produto invalido");
        else
            this.nomeProduto = nomeProduto;
    }

    public void setCategoria(String categoria) {
        if (categoria == null|| categoria.isBlank())
            throw new IllegalArgumentException("Nome do produto invalido");
        else
           this.categoria = categoria;
    }

    public void setQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0)
            throw new IllegalArgumentException("A quantidade invalida");
        else
            this.quantidade = quantidade;
    }

    public void setUnidadeDeMedida(String unidadeDeMedida) {
        if(unidadeDeMedida == null || unidadeDeMedida.isBlank())
            throw new IllegalArgumentException("A unidadede de media invalida");
        else
            this.unidadeDeMedida = unidadeDeMedida;
    }

    public void setPreco(BigDecimal preco) {
        if (preco == null|| preco.signum() < 0)
            throw new IllegalArgumentException("A preço invalido");
        else
            this.preco = preco;
    }

    public void setDataValidade(LocalDate dataValidade) {
        if (dataValidade == null)
            throw new IllegalArgumentException("A data está invalida");
        else
            this.dataValidade = dataValidade;
    }

    public void estoqueBaixa(Integer qntbaixa){
        if (qntbaixa == null || qntbaixa <= 0)
            throw new IllegalArgumentException("A quantidade invalida");
        else if (qntbaixa > quantidade)
            throw new IllegalArgumentException("insuficiente");
        else
            this.quantidade -= qntbaixa;
    }
}

