
package com.example.aep;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movimentacoes")
public class ControleMovimentacao {

    @PersistenceContext
    private EntityManager em;

    // REGISTRAR DOAÇÃO
    @PostMapping("/doacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrarDoacao(
            @RequestBody Map<String, String> body) {

        try {
            Integer loteId = Integer.parseInt(
                    body.getOrDefault("loteId", ""));

            Integer quantidade = Integer.parseInt(
                    body.getOrDefault("quantidade", ""));

            Integer usuarioId = Integer.parseInt(
                    body.getOrDefault("usuarioId", ""));

            String dataTexto = body.get("data");

            LocalDate data = (dataTexto == null || dataTexto.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(dataTexto);

            String entidade = body.get("entidades");

            Lote lote = em.find(Lote.class, loteId);

            if (lote == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lote não encontrado.");
            }

            Usuario usuario = em.find(Usuario.class, usuarioId);

            if (usuario == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }

            Doacao doacao = new Doacao(
                    lote,
                    quantidade,
                    data,
                    entidade,
                    usuario
            );

            em.persist(doacao);
            em.flush();

            return Map.of(
                    "mensagem", "Doação registrada com sucesso.",
                    "id", doacao.getId(),
                    "produto", lote.getNomeProduto(),
                    "quantidade", doacao.getQuantidade()
            );

        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verifique o lote, a quantidade, o usuário e a data.");

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // REGISTRAR DESCARTE
    @PostMapping("/descartes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrarDescarte(
            @RequestBody Map<String, String> body) {

        try {
            Integer loteId = Integer.parseInt(
                    body.getOrDefault("loteId", ""));

            Integer quantidade = Integer.parseInt(
                    body.getOrDefault("quantidade", ""));

            Integer usuarioId = Integer.parseInt(
                    body.getOrDefault("usuarioId", ""));

            String dataTexto = body.get("data");

            LocalDate data = (dataTexto == null || dataTexto.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(dataTexto);

            String justificativa = body.get("justificativa");

            Lote lote = em.find(Lote.class, loteId);

            if (lote == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lote não encontrado.");
            }

            Usuario usuario = em.find(Usuario.class, usuarioId);

            if (usuario == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado.");
            }

            Descarte descarte = new Descarte(
                    lote,
                    data,
                    quantidade,
                    justificativa,
                    usuario
            );

            em.persist(descarte);
            em.flush();

            return Map.of(
                    "mensagem", "Descarte registrado com sucesso.",
                    "id", descarte.getId(),
                    "produto", lote.getNomeProduto(),
                    "quantidade", descarte.getQuantidade()
            );

        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Verifique o lote, a quantidade, o usuário e a data.");

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // LISTAR DOAÇÕES
    @GetMapping("/doacoes")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDoacoes() {

        return em.createQuery(
                "SELECT d FROM Doacao d ORDER BY d.date DESC",
                Doacao.class
        ).getResultList().stream().map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "tipo", d.getTipo(),
                "produto", d.getPossui().getNomeProduto(),
                "loteId", d.getPossui().getId(),
                "quantidade", d.getQuantidade(),
                "data", d.getDate().toString(),
                "entidades", d.getEntidades() == null
                        ? "" : d.getEntidades(),
                "impacto", d.caulcularImpaquito()
        )).toList();
    }

    // LISTAR DESCARTES
    @GetMapping("/descartes")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDescartes() {

        return em.createQuery(
                "SELECT d FROM Descarte d ORDER BY d.date DESC",
                Descarte.class
        ).getResultList().stream().map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "tipo", d.getTipo(),
                "produto", d.getPossui().getNomeProduto(),
                "loteId", d.getPossui().getId(),
                "quantidade", d.getQuantidade(),
                "data", d.getDate().toString(),
                "justificativa", d.getJustificativa() == null
                        ? "" : d.getJustificativa(),
                "impacto", d.caulcularImpaquito()
        )).toList();
    }
}
