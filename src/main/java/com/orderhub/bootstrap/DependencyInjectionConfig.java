package com.orderhub.bootstrap;

import com.orderhub.adapters.out.inmemory.*;
import com.orderhub.application.port.in.*;
import com.orderhub.application.port.out.*;
import com.orderhub.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root (ARQ-04, ARQ-63).
 * Apenas o bootstrap conhece as classes concretas de todos os adaptadores e casos de uso.
 */
@Configuration
public class DependencyInjectionConfig {

    @Bean
    public PedidoRepositoryInMemory pedidoRepository() {
        return new PedidoRepositoryInMemory();
    }

    @Bean
    public UnitOfWork unitOfWork() {
        return new UnitOfWorkInMemory();
    }

    @Bean
    public RelogioSistema relogio() {
        return new RelogioSistema();
    }

    @Bean
    public GeradorDeId geradorDeId() {
        return new GeradorDeIdUuid();
    }

    @Bean
    public PublicadorDeEventosInMemory publicadorDeEventos() {
        return new PublicadorDeEventosInMemory();
    }

    @Bean
    public CriarPedidoUseCase criarPedidoUseCase(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        GeradorDeId geradorDeId,
        Relogio relogio
    ) {
        return new CriarPedido(pedidoRepository, unitOfWork, publicadorDeEventos, geradorDeId, relogio);
    }

    @Bean
    public ReservarEstoqueUseCase reservarEstoqueUseCase(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        Relogio relogio
    ) {
        return new ReservarEstoque(pedidoRepository, unitOfWork, publicadorDeEventos, relogio);
    }

    @Bean
    public ConfirmarPagamentoUseCase confirmarPagamentoUseCase(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        Relogio relogio
    ) {
        return new ConfirmarPagamento(pedidoRepository, unitOfWork, publicadorDeEventos, relogio);
    }

    @Bean
    public CancelarPedidoUseCase cancelarPedidoUseCase(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        Relogio relogio
    ) {
        return new CancelarPedido(pedidoRepository, unitOfWork, publicadorDeEventos, relogio);
    }

    @Bean
    public ConsultarPedidoUseCase consultarPedidoUseCase(PedidoRepository pedidoRepository) {
        return new ConsultarPedido(pedidoRepository);
    }

    @Bean
    public ListarPedidosDoClienteUseCase listarPedidosDoClienteUseCase(PedidoRepository pedidoRepository) {
        return new ListarPedidosDoCliente(pedidoRepository);
    }

    @Bean
    public ProcessarWebhookDePagamentoUseCase processarWebhookDePagamentoUseCase(
        PedidoRepository pedidoRepository,
        UnitOfWork unitOfWork,
        PublicadorDeEventos publicadorDeEventos,
        GeradorDeId geradorDeId,
        Relogio relogio
    ) {
        return new ProcessarWebhookDePagamento(pedidoRepository, unitOfWork, publicadorDeEventos, geradorDeId, relogio);
    }
}

