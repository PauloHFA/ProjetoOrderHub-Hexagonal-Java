package com.orderhub.domain;

import com.orderhub.domain.shared.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Value Objects - Domain Rules")
class ValueObjectsTest {

    @Nested
    @DisplayName("Dinheiro (ARQ-11)")
    class DinheiroTests {
        @Test
        void deveCriarDinheiroComValorValido() {
            var d = Dinheiro.criar(new BigDecimal("10.50"));
            assertEquals(new BigDecimal("10.50"), d.getValor());
        }

        @Test
        void deveArredondarParaDuasCasasDecimais() {
            var d = Dinheiro.criar(10.555);
            assertEquals(new BigDecimal("10.56"), d.getValor());
        }

        @Test
        void deveSomarDinheiroCorretamente() {
            var a = Dinheiro.criar(10.0);
            var b = Dinheiro.criar(5.50);
            assertEquals(Dinheiro.criar(15.50), a.somar(b));
        }

        @Test
        void deveSubtrairDinheiroCorretamente() {
            var a = Dinheiro.criar(10.0);
            var b = Dinheiro.criar(4.0);
            assertEquals(Dinheiro.criar(6.0), a.subtrair(b));
        }

        @Test
        void deveMultiplicarPorQuantidade() {
            var preco = Dinheiro.criar(15.0);
            assertEquals(Dinheiro.criar(45.0), preco.multiplicar(3));
        }

        @Test
        void deveCompararValoresCorretamente() {
            var maior = Dinheiro.criar(100.0);
            var menor = Dinheiro.criar(50.0);
            assertTrue(maior.ehMaiorQue(menor));
            assertTrue(menor.ehMenorQue(maior));
            assertFalse(maior.igual(menor));
        }
    }

    @Nested
    @DisplayName("Quantidade (ARQ-11)")
    class QuantidadeTests {
        @Test
        void deveCriarQuantidadeValida() {
            var q = Quantidade.criar(5);
            assertEquals(5, q.getValor());
        }

        @Test
        void deveRejeitarQuantidadeMenorOuIgualAZero() {
            assertThrows(DomainError.InvalidArgumentException.class, () -> Quantidade.criar(0));
            assertThrows(DomainError.InvalidArgumentException.class, () -> Quantidade.criar(-1));
        }

        @Test
        void deveSomarQuantidades() {
            var q1 = Quantidade.criar(3);
            var q2 = Quantidade.criar(4);
            assertEquals(Quantidade.criar(7), q1.somar(q2));
        }
    }

    @Nested
    @DisplayName("Email (ARQ-11)")
    class EmailTests {
        @Test
        void deveCriarEmailValido() {
            var email = Email.criar("usuario@exemplo.com");
            assertEquals("usuario@exemplo.com", email.getValor());
        }

        @Test
        void deveNormalizarParaMinusculas() {
            var email = Email.criar("  USUARIO@Exemplo.COM  ");
            assertEquals("usuario@exemplo.com", email.getValor());
        }

        @Test
        void deveRejeitarEmailInvalido() {
            assertThrows(DomainError.InvalidArgumentException.class, () -> Email.criar("email-invalido"));
        }
    }

    @Nested
    @DisplayName("Instante (ARQ-12)")
    class InstanteTests {
        @Test
        void deveCriarInstanteValido() {
            var agora = Instant.now();
            var inst = Instante.de(agora);
            assertEquals(agora, inst.getValor());
        }

        @Test
        void deveCompararAntesEDepois() {
            var t1 = Instant.now();
            var t2 = t1.plus(1, ChronoUnit.HOURS);
            var i1 = Instante.de(t1);
            var i2 = Instante.de(t2);
            assertTrue(i1.ehAntesDe(i2));
            assertTrue(i2.ehDepoisDe(i1));
        }
    }

    @Nested
    @DisplayName("Identificadores Tipados (ARQ-14)")
    class IdentificadoresTests {
        @Test
        void deveCriarPedidoIdValido() {
            var id = Identificadores.PedidoId.gerar();
            assertNotNull(id.getValor());
            assertFalse(id.getValor().isBlank());
        }

        @Test
        void deveCriarClienteIdValido() {
            var id = Identificadores.ClienteId.criar("cli-123");
            assertEquals("cli-123", id.getValor());
        }

        @Test
        void deveRejeitarIdentificadorVazio() {
            assertThrows(DomainError.InvalidArgumentException.class, () -> Identificadores.ClienteId.criar("  "));
            assertThrows(DomainError.InvalidArgumentException.class, () -> Identificadores.Sku.criar(""));
        }
    }
}

