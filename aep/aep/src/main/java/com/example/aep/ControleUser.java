package com.example.aep;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class ControleUser {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        return em.createQuery("select u from Usuario u order by u.id", Usuario.class)
                .getResultList().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "nome", u.getNome(),
                        "email", u.getEmail(),
                        "nivel", u instanceof Administrador ? "ADMINISTRADOR" : "OPERADOR"))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrar(@RequestBody Map<String, String> body) {
        String nivel = body.getOrDefault("nivel", "").trim().toUpperCase();

        // nome e email são validados dentro dos construtores
        Usuario usuario;
        try {
            usuario = switch (nivel) {
                case "ADMINISTRADOR" -> new Administrador(body.get("nome"), body.get("email"));
                case "OPERADOR" -> new Operador(body.get("nome"), body.get("email"));
                default -> throw new IllegalArgumentException("O nível deve ser Administrador ou Operador.");
            };
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Long existe = em.createQuery("select count(u) from Usuario u where u.email = :e", Long.class)
                .setParameter("e", usuario.getEmail())
                .getSingleResult();
        if (existe > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse email.");
        }

        em.persist(usuario);
        return Map.of("id", usuario.getId(), "mensagem", "Usuário cadastrado.");
    }
}