package com.orderhub.adapters.out.inmemory;

import com.orderhub.application.port.out.UnitOfWork;

import java.util.function.Supplier;

public class UnitOfWorkInMemory implements UnitOfWork {

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return operacao.get();
    }

    @Override
    public void executar(Runnable operacao) {
        operacao.run();
    }
}

