package com.example.aep;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("OPERADOR")
public class Operador extends Usuario{

    protected Operador(){
    }
    public Operador(String nome, String email) {
        super(nome, email);
    }
}
