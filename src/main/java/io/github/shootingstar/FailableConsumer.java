package io.github.shootingstar;

@FunctionalInterface
public interface FailableConsumer<T> {
    void accept(T o) throws Exception;
}