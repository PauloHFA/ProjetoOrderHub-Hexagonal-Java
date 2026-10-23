package com.orderhub.application.port.out;

import com.orderhub.domain.pedido.Pedido;
import com.orderhub.domain.shared.Identificadores.ClienteId;
import com.orderhub.domain.shared.Identificadores.PedidoId;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para persistência de Pedidos (ARQ-30, ARQ-31).
 * Pequena e sob medida para o domínio, sem CRUD genérico.
 */
public interface PedidoRepository {
    Optional<Pedido> buscarPorId(PedidoId id);
    List<Pedido> buscarPorClienteId(ClienteId clienteId, int pagina, int tamanho);
    int contarPorClienteId(ClienteId clienteId);
    void salvar(Pedido pedido);
    boolean existePorId(PedidoId id);

    // Idempotência
    boolean existeChaveIdempotencia(String chave);
    void registrarChaveIdempotencia(String chave, PedidoId pedidoId, long ttlSegundos);
}

