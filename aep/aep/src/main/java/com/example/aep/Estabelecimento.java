package com.example.aep;
import jakarta.persistence.*;
import java.util.ArrayList;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "estabelecimento")
public class Estabelecimento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Administrador administrador;
    @Column(name = "nome_estabelecimento", nullable = false, length = 200)
    private String nome;
    @Column(length = 200)
    private String codigo;
    @OneToMany(mappedBy = "estabelecimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lote> lotes = new ArrayList<>();
    protected Estabelecimento() {
    }
    public Estabelecimento(Administrador administrador, String nome, String codigo) {
        if(administrador == null && nome == null||nome.isBlank()){
            throw new IllegalArgumentException("Adiministrador Invalido");
        }else{
            this.administrador = administrador;
            this.nome = nome;
            this.codigo = codigo;
        }
    }

    public Integer getId() {
        return id;
    }

    public Administrador getAdministrador() {
        return administrador;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<Lote> getLotes() {
        return lotes;
    }

    public void adicionarLotes(Estabelecimento estabelecimento, Operador registrarBaixa, String nomeProduto, String categoria, Integer quantidade, String unidadeDeMedida, BigDecimal preco, LocalDate dataValidade){
        Lote lote = new Lote(estabelecimento,registrarBaixa, nomeProduto, categoria,quantidade,unidadeDeMedida,preco,dataValidade);
        lotes.add(lote);
    }

}