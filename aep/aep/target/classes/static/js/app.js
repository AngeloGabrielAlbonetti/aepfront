const API_BASE = '/api';

let lotes = [];
let avisoTimer;

// REQUISIÇÕES À API

async function api(url, options = {}) {
    const response = await fetch(url, options);

    let data = null;

    try {
        data = await response.json();
    } catch (_) {}

    if (!response.ok) {
        throw new Error(
            (data && (data.erro || data.message || data.detail)) ||
            `Erro ${response.status}`
        );
    }

    return data;
}

function enviar(metodo, url, corpo) {
    return api(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(corpo)
    });
}

// FUNÇÕES AUXILIARES

function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, char =>
        ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        })[char]
    );
}

function mostrarAviso(texto, sucesso = true) {
    const el = document.getElementById('aviso');

    el.textContent = texto;
    el.className = `toast-message ${sucesso ? 'ok' : 'erro'} show`;

    clearTimeout(avisoTimer);

    avisoTimer = setTimeout(() => {
        el.classList.remove('show');
    }, 3500);
}

function dataBR(iso) {
    if (!iso) return '—';

    const [ano, mes, dia] = String(iso).slice(0, 10).split('-');

    return `${dia}/${mes}/${ano}`;
}

function dataHoje() {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    const dia = String(hoje.getDate()).padStart(2, '0');

    return `${ano}-${mes}-${dia}`;
}

function moeda(valor) {
    return Number(valor ?? 0).toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });
}

// NAVEGAÇÃO

function mostrar(aba) {
    const abas = [
        'lotes',
        'estabelecimentos',
        'usuarios',
        'movimentacoes'
    ];

    abas.forEach(nome => {
        const painel = document.getElementById(`tab-${nome}`);
        const botao = document.querySelector(`[data-tab="${nome}"]`);

        if (painel) {
            painel.classList.toggle('active', nome === aba);
        }

        if (botao) {
            botao.classList.toggle('active', nome === aba);
        }
    });

    const titulos = {
        lotes: 'Visão geral',
        estabelecimentos: 'Estabelecimentos',
        usuarios: 'Usuários',
        movimentacoes: 'Doações e descartes'
    };

    document.getElementById('page-title').textContent =
        titulos[aba] || 'Visão geral';

    document.getElementById('sidebar').classList.remove('open');

    if (aba === 'movimentacoes') {
        carregarMovimentacoes();
        atualizarSelectLotes();
    }
}

// USUÁRIOS

async function carregarUsuarios() {
    try {
        const resposta = await api(`${API_BASE}/usuarios`);

        const usuarios = Array.isArray(resposta)
            ? resposta
            : resposta.usuarios || resposta.content || [];

        console.log('Usuários:', usuarios);

        document.getElementById('tabela-usuarios').innerHTML =
            usuarios.length
                ? usuarios.map(u => `
                    <tr>
                        <td>${u.id}</td>
                        <td class="product-name">${esc(u.nome)}</td>
                        <td>${esc(u.email)}</td>
                        <td>${esc(u.nivel || u.tipo || u.role || 'Não informado')}</td>
                    </tr>
                `).join('')
                : '<tr><td colspan="4">Nenhum usuário cadastrado.</td></tr>';

        const admins = usuarios.filter(u =>
            String(u.nivel || u.tipo || u.role || '')
                .toUpperCase().includes('ADMIN')
        );

        document.getElementById('est-administrador').innerHTML =
            '<option value="">Selecione um administrador</option>' +
            admins.map(u =>
                `<option value="${u.id}">${esc(u.nome)}</option>`
            ).join('');

        const operadores = usuarios.filter(u =>
            String(u.nivel || u.tipo || u.role || '')
                .toUpperCase().includes('OPER')
        );

        const selectOperador = document.getElementById('lote-operador');

        selectOperador.innerHTML =
            '<option value="">Selecione um operador</option>' +
            operadores.map(u =>
                `<option value="${u.id}">${esc(u.nome)} - Operador</option>`
            ).join('');

        const opcoesOperadores =
            '<option value="">Selecione o responsável</option>' +
            operadores.map(u =>
                `<option value="${u.id}">${esc(u.nome)}</option>`
            ).join('');

        ['doacao-usuario', 'descarte-usuario'].forEach(id => {
            const sel = document.getElementById(id);
            if (!sel) return;
            const atual = sel.value;
            sel.innerHTML = opcoesOperadores;
            if (operadores.some(u => String(u.id) === atual)) sel.value = atual;
        });

        console.log('Operadores encontrados:', operadores);

    } catch (error) {
        console.error('Erro ao carregar usuários:', error);
        mostrarAviso(`Não foi possível carregar usuários: ${error.message}`, false);
    }
}

// ESTABELECIMENTOS

async function carregarEstabelecimentos() {
    try {
        const lista = await api(`${API_BASE}/estabelecimentos`);

        document.getElementById('tabela-estabelecimentos').innerHTML =
            lista.length
                ? lista.map(e => `
          <tr>
            <td>${e.id}</td>
            <td class="product-name">${esc(e.nome)}</td>
            <td>${esc(e.codigo || '—')}</td>
            <td>${esc(e.administrador || '—')}</td>
          </tr>
        `).join('')
                : '<tr><td colspan="4" class="empty-cell">Nenhum estabelecimento cadastrado.</td></tr>';

        document.getElementById('lote-estabelecimento').innerHTML =
            lista.length
                ? '<option value="">Selecione um estabelecimento</option>' +
                lista.map(e =>
                    `<option value="${e.id}">${esc(e.nome)}</option>`
                ).join('')
                : '<option value="">Cadastre um estabelecimento primeiro</option>';

    } catch (error) {
        mostrarAviso(
            `Não foi possível carregar estabelecimentos: ${error.message}`,
            false
        );
    }
}

// LOTES

const rotulos = {
    VENCIDO: 'Vencido',
    PROXIMO: 'Próximo do vencimento',
    OK: 'Dentro do prazo'
};

async function carregarLotes() {
    try {
        const lista = await api(`${API_BASE}/lotes`);

        lotes = lista.map(l => ({
            ...l,
            situacao: l.vencido
                ? 'VENCIDO'
                : (Number(l.dias) <= 3 ? 'PROXIMO' : 'OK')
        }));

        desenharLotes();
        atualizarIndicadores();
        atualizarSelectLotes();

    } catch (error) {
        document.getElementById('tabela-lotes').innerHTML =
            '<tr><td colspan="8" class="empty-cell">Não foi possível carregar os lotes. Verifique se o servidor está ativo.</td></tr>';

        mostrarAviso(`Erro ao carregar lotes: ${error.message}`, false);
    }
}

function atualizarIndicadores() {
    const total = lotes.length;

    const contar = situacao =>
        lotes.filter(l => l.situacao === situacao).length;

    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-ok').textContent = contar('OK');
    document.getElementById('stat-proximo').textContent = contar('PROXIMO');
    document.getElementById('stat-vencido').textContent = contar('VENCIDO');
    document.getElementById('nav-lotes-count').textContent = total;
}

function desenharLotes() {
    const filtro = document.getElementById('filtro').value;

    const visiveis = lotes.filter(l =>
        filtro === 'TODOS' || l.situacao === filtro
    );

    document.getElementById('tabela-lotes').innerHTML =
        visiveis.length
            ? visiveis.map(l => `
        <tr>
          <td class="product-name">${esc(l.produto)}</td>
          <td>${esc(l.quantidade)} ${esc(l.unidade)}</td>
          <td>${moeda(l.preco)}</td>
          <td>
            ${dataBR(l.validade)}
            <span class="muted">(${esc(l.dias)} dias)</span>
          </td>
          <td>
            <span class="status-pill ${l.situacao}">
              ${rotulos[l.situacao]}
            </span>
          </td>
          <td>${esc(l.estabelecimento)}</td>
          <td>${esc(l.operador || '—')}</td>
          <td>
            <div class="row-actions">
              <button class="action-btn edit" data-edit="${l.id}">Editar</button>
              <button class="action-btn delete" data-delete="${l.id}">Excluir</button>
            </div>
          </td>
        </tr>
      `).join('')
            : '<tr><td colspan="8" class="empty-cell">Nenhum lote encontrado para este filtro.</td></tr>';
}

function editarLote(id) {
    const lote = lotes.find(item => Number(item.id) === Number(id));

    if (!lote) return;

    document.getElementById('lote-id').value = lote.id;
    document.getElementById('lote-produto').value = lote.produto || '';
    document.getElementById('lote-categoria').value = lote.categoria || '';
    document.getElementById('lote-quantidade').value = lote.quantidade ?? '';
    document.getElementById('lote-unidade').value = lote.unidade || '';
    document.getElementById('lote-preco').value =
        Number(lote.preco || 0).toFixed(2);

    document.getElementById('lote-validade').value =
        String(lote.validade || '').slice(0, 10);

    document.getElementById('lote-estabelecimento').value =
        lote.estabelecimentoId ||
        document.getElementById('lote-estabelecimento').value;

    document.getElementById('lote-operador').value =
        lote.operadorId || '';

    document.getElementById('lote-estabelecimento').disabled = true;
    document.getElementById('lote-operador').disabled = false;

    document.getElementById('titulo-lote').textContent =
        `Editar lote nº ${lote.id}`;

    document.getElementById('btn-salvar-lote').textContent =
        'Salvar alterações';

    document.getElementById('btn-cancelar-lote').hidden = false;

    document.getElementById('lote-editor').scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });
}

function cancelarEdicao() {
    document.getElementById('form-lote').reset();
    document.getElementById('lote-id').value = '';

    document.getElementById('lote-estabelecimento').disabled = false;
    document.getElementById('lote-operador').disabled = false;

    document.getElementById('titulo-lote').textContent =
        'Cadastrar novo lote';

    document.getElementById('btn-salvar-lote').textContent =
        'Salvar lote';

    document.getElementById('btn-cancelar-lote').hidden = true;
}

async function excluirLote(id) {
    if (!confirm(`Deseja realmente excluir o lote nº ${id}?`)) return;

    try {
        await api(`${API_BASE}/lotes/${id}`, {
            method: 'DELETE'
        });

        mostrarAviso('Lote excluído com sucesso!');

        await carregarLotes();

    } catch (error) {
        mostrarAviso(error.message, false);
    }
}

// SELECT DE LOTES PARA DOAÇÃO E DESCARTE

function atualizarSelectLotes() {
    const selects = [
        document.getElementById('doacao-lote'),
        document.getElementById('descarte-lote')
    ];

    selects.forEach(select => {
        if (!select) return;

        const selecionado = select.value;

        const disponiveis = lotes.filter(l =>
            Number(l.quantidade) > 0
        );

        select.innerHTML =
            '<option value="">Selecione um lote</option>' +
            disponiveis.map(l => `
        <option value="${l.id}">
          ${esc(l.produto)} — ${esc(l.quantidade)} ${esc(l.unidade)}
        </option>
      `).join('');

        if (disponiveis.some(l => String(l.id) === selecionado)) {
            select.value = selecionado;
        }
    });
}

// USUÁRIO RESPONSÁVEL PELA MOVIMENTAÇÃO

function obterOperadorDoLote(lote) {
    const operadorId = lote.operadorId;

    if (!operadorId) {
        throw new Error(
            'Este lote não possui um operador responsável cadastrado.'
        );
    }

    return String(operadorId);
}

// DOAÇÕES

async function cadastrarDoacao(event) {
    event.preventDefault();

    const loteId = document.getElementById('doacao-lote').value;

    const quantidade = Number(
        document.getElementById('doacao-quantidade').value
    );

    const data = document.getElementById('doacao-data').value;

    const entidades =
        document.getElementById('doacao-entidade').value.trim();

    const lote = lotes.find(l => Number(l.id) === Number(loteId));

    if (!lote) {
        mostrarAviso('Selecione um lote válido.', false);
        return;
    }

    if (!Number.isInteger(quantidade) || quantidade <= 0) {
        mostrarAviso('Informe uma quantidade válida.', false);
        return;
    }

    if (quantidade > Number(lote.quantidade)) {
        mostrarAviso(
            `Estoque insuficiente. Disponível: ${lote.quantidade}.`,
            false
        );
        return;
    }

    if (!data || !entidades) {
        mostrarAviso('Preencha todos os campos da doação.', false);
        return;
    }

    try {
        const usuarioId = document.getElementById('doacao-usuario').value;

        if (!usuarioId) {
            mostrarAviso('Selecione o usuário responsável (operador).', false);
            return;
        }

        await enviar(
            'POST',
            `${API_BASE}/movimentacoes/doacoes`,
            {
                loteId: String(loteId),
                quantidade: String(quantidade),
                data: data,
                entidades: entidades,
                usuarioId: usuarioId
            }
        );

        event.target.reset();

        document.getElementById('doacao-data').value = dataHoje();

        mostrarAviso('Doação registrada com sucesso!');

        await carregarLotes();
        await carregarDoacoes();

    } catch (error) {
        mostrarAviso(error.message, false);
    }
}

// DESCARTES

async function cadastrarDescarte(event) {
    event.preventDefault();

    const loteId = document.getElementById('descarte-lote').value;

    const quantidade = Number(
        document.getElementById('descarte-quantidade').value
    );

    const data = document.getElementById('descarte-data').value;

    const justificativa =
        document.getElementById('descarte-justificativa').value.trim();

    const lote = lotes.find(l => Number(l.id) === Number(loteId));

    if (!lote) {
        mostrarAviso('Selecione um lote válido.', false);
        return;
    }

    if (!Number.isInteger(quantidade) || quantidade <= 0) {
        mostrarAviso('Informe uma quantidade válida.', false);
        return;
    }

    if (quantidade > Number(lote.quantidade)) {
        mostrarAviso(
            `Estoque insuficiente. Disponível: ${lote.quantidade}.`,
            false
        );
        return;
    }

    if (!data || !justificativa) {
        mostrarAviso('Preencha todos os campos do descarte.', false);
        return;
    }

    try {
        const usuarioId = document.getElementById('descarte-usuario').value;

        if (!usuarioId) {
            mostrarAviso('Selecione o usuário responsável (operador).', false);
            return;
        }

        await enviar(
            'POST',
            `${API_BASE}/movimentacoes/descartes`,
            {
                loteId: String(loteId),
                quantidade: String(quantidade),
                data: data,
                justificativa: justificativa,
                usuarioId: usuarioId
            }
        );

        event.target.reset();

        document.getElementById('descarte-data').value = dataHoje();

        mostrarAviso('Descarte registrado com sucesso!');

        await carregarLotes();
        await carregarDescartes();

    } catch (error) {
        mostrarAviso(error.message, false);
    }
}

// HISTÓRICO DE DOAÇÕES

async function carregarDoacoes() {
    try {
        const doacoes = await api(
            `${API_BASE}/movimentacoes/doacoes`
        );

        document.getElementById('tabela-doacoes').innerHTML =
            doacoes.length
                ? doacoes.map(d => `
          <tr>
            <td>${d.id}</td>
            <td class="product-name">${esc(d.produto)}</td>
            <td>${esc(d.quantidade)}</td>
            <td>${dataBR(d.data)}</td>
            <td>${esc(d.entidades)}</td>
            <td>${moeda(d.impacto)}</td>
          </tr>
        `).join('')
                : '<tr><td colspan="6" class="empty-cell">Nenhuma doação registrada.</td></tr>';

    } catch (error) {
        document.getElementById('tabela-doacoes').innerHTML =
            '<tr><td colspan="6" class="empty-cell">Não foi possível carregar as doações.</td></tr>';

        mostrarAviso(`Erro ao carregar doações: ${error.message}`, false);
    }
}

// HISTÓRICO DE DESCARTES

async function carregarDescartes() {
    try {
        const descartes = await api(
            `${API_BASE}/movimentacoes/descartes`
        );

        document.getElementById('tabela-descartes').innerHTML =
            descartes.length
                ? descartes.map(d => `
          <tr>
            <td>${d.id}</td>
            <td class="product-name">${esc(d.produto)}</td>
            <td>${esc(d.quantidade)}</td>
            <td>${dataBR(d.data)}</td>
            <td>${esc(d.justificativa)}</td>
            <td>${moeda(d.impacto)}</td>
          </tr>
        `).join('')
                : '<tr><td colspan="6" class="empty-cell">Nenhum descarte registrado.</td></tr>';

    } catch (error) {
        document.getElementById('tabela-descartes').innerHTML =
            '<tr><td colspan="6" class="empty-cell">Não foi possível carregar os descartes.</td></tr>';

        mostrarAviso(`Erro ao carregar descartes: ${error.message}`, false);
    }
}

async function carregarMovimentacoes() {
    await Promise.all([
        carregarDoacoes(),
        carregarDescartes()
    ]);
}

// INICIALIZAÇÃO E EVENTOS

document.addEventListener('DOMContentLoaded', () => {

    document.getElementById('today-label').textContent =
        new Date().toLocaleDateString('pt-BR', {
            weekday: 'short',
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });

    document.getElementById('doacao-data').value = dataHoje();
    document.getElementById('descarte-data').value = dataHoje();

    // Navegação

    document.querySelectorAll('[data-tab]').forEach(button => {
        button.addEventListener('click', () => {
            mostrar(button.dataset.tab);
        });
    });

    document.getElementById('mobile-menu').addEventListener('click', () => {
        document.getElementById('sidebar').classList.toggle('open');
    });

    document.getElementById('focus-lote').addEventListener('click', () => {
        document.getElementById('lote-editor').scrollIntoView({
            behavior: 'smooth'
        });
    });

    document.getElementById('filtro').addEventListener('change', desenharLotes);

    document.getElementById('btn-cancelar-lote').addEventListener(
        'click',
        cancelarEdicao
    );

    // Ações da tabela de lotes

    document.getElementById('tabela-lotes').addEventListener('click', event => {
        const editar = event.target.closest('[data-edit]');
        const excluir = event.target.closest('[data-delete]');

        if (editar) editarLote(editar.dataset.edit);
        if (excluir) excluirLote(excluir.dataset.delete);
    });

    // Cadastro de usuários

    document.getElementById('form-usuario').addEventListener(
        'submit',
        async event => {
            event.preventDefault();

            try {
                await enviar('POST', `${API_BASE}/usuarios`, {
                    nome: document.getElementById('usu-nome').value,
                    email: document.getElementById('usu-email').value,
                    nivel: document.getElementById('usu-nivel').value
                });

                event.target.reset();

                mostrarAviso('Usuário cadastrado com sucesso!');

                await carregarUsuarios();

            } catch (error) {
                mostrarAviso(error.message, false);
            }
        }
    );

    // Cadastro de estabelecimentos

    document.getElementById('form-estabelecimento').addEventListener(
        'submit',
        async event => {
            event.preventDefault();

            try {
                await enviar('POST', `${API_BASE}/estabelecimentos`, {
                    administradorId:
                    document.getElementById('est-administrador').value,
                    nome: document.getElementById('est-nome').value,
                    codigo: document.getElementById('est-codigo').value
                });

                event.target.reset();

                mostrarAviso('Estabelecimento cadastrado com sucesso!');

                await carregarEstabelecimentos();

            } catch (error) {
                mostrarAviso(error.message, false);
            }
        }
    );

    // Cadastro e edição de lotes

    document.getElementById('form-lote').addEventListener(
        'submit',
        async event => {
            event.preventDefault();

            const id = document.getElementById('lote-id').value;

            const corpo = {
                estabelecimentoId:
                document.getElementById('lote-estabelecimento').value,
                operadorId:
                document.getElementById('lote-operador').value,
                nomeProduto:
                document.getElementById('lote-produto').value,
                categoria:
                document.getElementById('lote-categoria').value,
                quantidade:
                document.getElementById('lote-quantidade').value,
                unidadeDeMedida:
                document.getElementById('lote-unidade').value,
                preco:
                document.getElementById('lote-preco').value,
                dataValidade:
                document.getElementById('lote-validade').value
            };

            try {
                if (id) {
                    await enviar('PUT', `${API_BASE}/lotes/${id}`, corpo);

                    mostrarAviso('Lote atualizado com sucesso!');

                } else {
                    await enviar('POST', `${API_BASE}/lotes`, corpo);

                    mostrarAviso('Lote cadastrado com sucesso!');
                }

                cancelarEdicao();

                await carregarLotes();

            } catch (error) {
                mostrarAviso(error.message, false);
            }
        }
    );

    // Cadastro de doações

    document.getElementById('form-doacao').addEventListener(
        'submit',
        cadastrarDoacao
    );

    // Cadastro de descartes

    document.getElementById('form-descarte').addEventListener(
        'submit',
        cadastrarDescarte
    );

    // Carregamento inicial

    carregarUsuarios();
    carregarEstabelecimentos();
    carregarLotes();
    carregarMovimentacoes();
});