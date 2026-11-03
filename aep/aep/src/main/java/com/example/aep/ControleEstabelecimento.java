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
@RequestMapping("/api/estabelecimentos")
public class ControleEstabelecimento {
    @PersistenceContext
    private EntityManager em;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        return em.createQuery("select e from Estabelecimento e order by e.id", Estabelecimento.class)
                .getResultList().stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "nome", e.getNome(),
                        "codigo", e.getCodigo() == null ? "" : e.getCodigo(),
                        "administrador", e.getAdministrador().getNome()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrar(@RequestBody Map<String, String> body) {
        int administradorId;
        try {
            administradorId = Integer.parseInt(body.getOrDefault("administradorId", ""));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um administrador.");
        }
        Administrador adm = em.find(Administrador.class, administradorId);
        if (adm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrador não encontrado.");
        }

        Estabelecimento est;
        try {
            est = new Estabelecimento(adm, body.get("nome"), body.get("codigo"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        em.persist(est);
        return Map.of("id", est.getId(), "mensagem", "Estabelecimento cadastrado.");
    }
}

