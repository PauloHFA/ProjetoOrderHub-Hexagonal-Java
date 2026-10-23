package com.orderhub.application.port.out;

import java.util.function.Supplier;

/**
 * Porta de saída para controle de fronteira transacional (ARQ-24).
 * O caso de uso orquestra a transação através desta abstração.
 */
public interface UnitOfWork {
    <T> T executar(Supplier<T> operacao);
    void executar(Runnable operacao);
}

