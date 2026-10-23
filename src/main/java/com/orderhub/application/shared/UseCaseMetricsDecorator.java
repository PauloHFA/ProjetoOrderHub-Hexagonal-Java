package com.orderhub.application.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Decorator para emissão de métricas em casos de uso (ARQ-71).
 * Não polui o caso de uso e intercepta latência e taxas de sucesso/erro.
 */
public final class UseCaseMetricsDecorator {

    public interface MetricsCollector {
        void incrementarContador(String metrica);
        void registrarLatencia(String metrica, long latenciaMs);
    }

    public static class InMemoryMetricsCollector implements MetricsCollector {
        private final Map<String, AtomicInteger> contadores = new ConcurrentHashMap<>();
        private final Map<String, List<Long>> latencias = new ConcurrentHashMap<>();

        @Override
        public void incrementarContador(String metrica) {
            contadores.computeIfAbsent(metrica, k -> new AtomicInteger(0)).incrementAndGet();
        }

        @Override
        public void registrarLatencia(String metrica, long latenciaMs) {
            latencias.computeIfAbsent(metrica, k -> Collections.synchronizedList(new ArrayList<>())).add(latenciaMs);
        }

        public int obterContador(String metrica) {
            AtomicInteger val = contadores.get(metrica);
            return val != null ? val.get() : 0;
        }

        public List<Long> obterLatencias(String metrica) {
            return latencias.getOrDefault(metrica, List.of());
        }

        public void limpar() {
            contadores.clear();
            latencias.clear();
        }
    }

    public static <TCmd, TRes> Function<TCmd, Result<TRes, Exception>> decorar(
        String nomeOperacao,
        Function<TCmd, Result<TRes, Exception>> operacao,
        MetricsCollector collector
    ) {
        return cmd -> {
            long inicio = System.currentTimeMillis();
            collector.incrementarContador("usecase." + nomeOperacao + ".total");
            try {
                Result<TRes, Exception> res = operacao.apply(cmd);
                long duracao = System.currentTimeMillis() - inicio;
                collector.registrarLatencia("usecase." + nomeOperacao + ".latencia", duracao);

                if (res.isOk()) {
                    collector.incrementarContador("usecase." + nomeOperacao + ".sucesso");
                } else {
                    collector.incrementarContador("usecase." + nomeOperacao + ".falha");
                }
                return res;
            } catch (Exception e) {
                long duracao = System.currentTimeMillis() - inicio;
                collector.registrarLatencia("usecase." + nomeOperacao + ".latencia", duracao);
                collector.incrementarContador("usecase." + nomeOperacao + ".excecao");
                throw e;
            }
        };
    }
}

