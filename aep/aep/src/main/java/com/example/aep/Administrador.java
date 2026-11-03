package com.example.aep;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario{
    protected Administrador(){}

    public Administrador(String nome, String email) {
        super(nome, email);
    }


}
