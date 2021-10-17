package io.github.shootingstar;

import java.util.function.Consumer;

public class Util {

    private Util() {}

    public static <T> Consumer<T> uncheckedConsumer(FailableConsumer<T> failedConsumer) {
        return o -> {
            try {
                failedConsumer.accept(o);
            } catch (Exception e) {
                Util.<RuntimeException>uncheckedThrow(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}