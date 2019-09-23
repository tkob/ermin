package yokohama.lang.ermin.front;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

public interface Resolver<K, V> {
    Optional<V> resolve(K name);

    default V resolveOrThrow(K name) {
        return resolve(name).orElseThrow(() -> new NoSuchElementException(name + " not found"));
    }

    default boolean hasName(K name) {
        return resolve(name).isPresent();
    }

    default void ifResolvedOrElse(K name, Consumer<? super V> action, Runnable emptyAction) {
        if (hasName(name)) {
            action.accept(resolveOrThrow(name));
        } else {
            emptyAction.run();
        }
    }
}
