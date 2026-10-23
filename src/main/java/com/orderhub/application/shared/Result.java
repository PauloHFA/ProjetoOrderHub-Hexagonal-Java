package com.orderhub.application.shared;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Representação funcional de sucesso ou falha tipada (ARQ-25).
 * Implementado via Sealed Interface do Java 21.
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    boolean isOk();
    boolean isErr();
    T getValue();
    E getError();

    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        if (isOk()) {
            return ok(mapper.apply(getValue()));
        }
        return err(getError());
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        if (isOk()) {
            return mapper.apply(getValue());
        }
        return err(getError());
    }

    default void ifOk(Consumer<? super T> action) {
        if (isOk()) {
            action.accept(getValue());
        }
    }

    default void ifErr(Consumer<? super E> action) {
        if (isErr()) {
            action.accept(getError());
        }
    }

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override public boolean isOk() { return true; }
        @Override public boolean isErr() { return false; }
        @Override public T getValue() { return value; }
        @Override public E getError() { throw new IllegalStateException("Called getError() on Ok"); }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err {
            Objects.requireNonNull(error, "error cannot be null");
        }
        @Override public boolean isOk() { return false; }
        @Override public boolean isErr() { return true; }
        @Override public T getValue() { throw new IllegalStateException("Called getValue() on Err: " + error); }
        @Override public E getError() { return error; }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <E> Result<Void, E> ok() {
        return new Ok<>(null);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }
}

