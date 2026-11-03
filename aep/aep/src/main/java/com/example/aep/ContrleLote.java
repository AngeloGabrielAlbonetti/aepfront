package com.example.aep;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lotes")
public class ContrleLote {
    @PersistenceContext
    private EntityManager em;

    // READ
    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        return em.createQuery("select l from Lote l order by l.dataValidade", Lote.class)
                .getResultList().stream()
                .map(this::converter)
                .toList();
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> cadastrar(@RequestBody Map<String, String> body) {
        int estabelecimentoId;
        Integer operadorId = null;
        int quantidade;
        BigDecimal preco;
        LocalDate validade;
        try {
            estabelecimentoId = Integer.parseInt(body.getOrDefault("estabelecimentoId", ""));
            String op = body.getOrDefault("operadorId", "");
            if (!op.isBlank()) {
                operadorId = Integer.parseInt(op);
            }
            quantidade = Integer.parseInt(body.getOrDefault("quantidade", ""));
            preco = new BigDecimal(body.getOrDefault("preco", "").replace(",", "."));
            validade = LocalDate.parse(body.getOrDefault("dataValidade", ""));
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verifique estabelecimento, quantidade, preço e data de validade.");
        }

        Estabelecimento est = em.find(Estabelecimento.class, estabelecimentoId);
        if (est == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado.");
        }
        Operador operador = null;
        if (operadorId != null) {
            operador = em.find(Operador.class, operadorId);
            if (operador == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operador não encontrado.");
            }
        }

        // composição do UML: o estabelecimento adiciona o lote (a validação está no construtor do Lote)
        try {
            est.adicionarLotes(est, operador, body.get("nomeProduto"), body.get("categoria"),
                    quantidade, body.get("unidadeDeMedida"), preco, validade);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return Map.of("mensagem", "Lote cadastrado.");
    }

    // UPDATE
    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> atualizar(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Lote lote = em.find(Lote.class, id);
        if (lote == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado.");
        }

        int quantidade;
        BigDecimal preco;
        LocalDate validade;
        try {
            quantidade = Integer.parseInt(body.getOrDefault("quantidade", ""));
            preco = new BigDecimal(body.getOrDefault("preco", "").replace(",", "."));
            validade = LocalDate.parse(body.getOrDefault("dataValidade", ""));
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verifique quantidade, preço e data de validade.");
        }

        // cada setter valida o próprio campo; se algum falhar, a transação inteira é desfeita
        try {
            lote.setNomeProduto(body.get("nomeProduto"));
            lote.setCategoria(body.get("categoria"));
            lote.setQuantidade(quantidade);
            lote.setUnidadeDeMedida(body.get("unidadeDeMedida"));
            lote.setPreco(preco);
            lote.setDataValidade(validade);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return Map.of("mensagem", "Lote atualizado.");
    }

    // DELETE
    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> excluir(@PathVariable Integer id) {
        Lote lote = em.find(Lote.class, id);
        if (lote == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado.");
        }
        try {
            em.remove(lote);
            em.flush();
        } catch (PersistenceException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: o lote já tem doação ou descarte registrado.");
        }
        return Map.of("mensagem", "Lote excluído.");
    }

    private Map<String, Object> converter(Lote l) {
        Operador op = l.getRegistrarBaixa();
        return Map.ofEntries(
                Map.entry("id", l.getId()),
                Map.entry("produto", l.getNomeProduto()),
                Map.entry("categoria", l.getCategoria() == null ? "" : l.getCategoria()),
                Map.entry("quantidade", l.getQuantidade()),
                Map.entry("unidade", l.getUnidadeDeMedida() == null ? "" : l.getUnidadeDeMedida()),
                Map.entry("preco", l.getPreco() == null ? BigDecimal.ZERO : l.getPreco()),
                Map.entry("validade", l.getDataValidade().toString()),
                Map.entry("dias", l.tempoPraVencimento()),
                Map.entry("vencido", l.vencido()),
                Map.entry("operador", op == null ? "-" : op.getNome()),
                Map.entry("estabelecimento", l.getEstabelecimento().getNome()));
    }
}

