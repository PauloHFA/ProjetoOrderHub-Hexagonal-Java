package com.orderhub.adapters.out.inmemory;

import com.orderhub.application.port.out.PedidoRepository;
import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PedidoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistência em memória para testes (ARQ-62).
 */
public class PedidoRepositoryInMemory implements PedidoRepository {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
    private final Map<String, ChaveIdempotenciaRegistro> chaves = new ConcurrentHashMap<>();

    private record ChaveIdempotenciaRegistro(String pedidoId, Instant expiraEm) {}

    @Override
    public Optional<Pedido> buscarPorId(PedidoId id) {
        return Optional.ofNullable(pedidos.get(id.getValor()));
    }

    @Override
    public List<Pedido> buscarPorClienteId(ClienteId clienteId, int pagina, int tamanho) {
        List<Pedido> doCliente = pedidos.values().stream()
            .filter(p -> p.getClienteId().igual(clienteId))
            .toList();

        int inicio = (pagina - 1) * tamanho;
        if (inicio >= doCliente.size()) return List.of();
        int fim = Math.min(inicio + tamanho, doCliente.size());
        return doCliente.subList(inicio, fim);
    }

    @Override
    public int contarPorClienteId(ClienteId clienteId) {
        return (int) pedidos.values().stream()
            .filter(p -> p.getClienteId().igual(clienteId))
            .count();
    }

    @Override
    public void salvar(Pedido pedido) {
        pedidos.put(pedido.getId().getValor(), pedido);
    }

    @Override
    public boolean existePorId(PedidoId id) {
        return pedidos.containsKey(id.getValor());
    }

    @Override
    public boolean existeChaveIdempotencia(String chave) {
        ChaveIdempotenciaRegistro reg = chaves.get(chave);
        if (reg == null) return false;
        if (reg.expiraEm.isBefore(Instant.now())) {
            chaves.remove(chave);
            return false;
        }
        return true;
    }

    @Override
    public void registrarChaveIdempotencia(String chave, PedidoId pedidoId, long ttlSegundos) {
        chaves.put(chave, new ChaveIdempotenciaRegistro(
            pedidoId != null ? pedidoId.getValor() : null,
            Instant.now().plusSeconds(ttlSegundos)
        ));
    }

    public void limpar() {
        pedidos.clear();
        chaves.clear();
    }

    public int tamanho() {
        return pedidos.size();
    }
}

