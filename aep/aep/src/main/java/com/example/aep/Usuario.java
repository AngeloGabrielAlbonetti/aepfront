package com.example.aep;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "nivel_do_usuario", length = 20)
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 200)
    private String nome;
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    protected Usuario() {}

    public Usuario(String nome, String email) {
        if(nome== null||nome.isBlank() && email == null||!email.contains("@")){
            throw new IllegalArgumentException("Usuario Invalido");
        }else{
            this.nome = nome;
            this.email = email;
        }
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }


}