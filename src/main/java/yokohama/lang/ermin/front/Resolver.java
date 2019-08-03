package yokohama.lang.ermin.front;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

public interface Resolver<T> {
    Optional<T> resolve(String name);

    default T resolveOrThrow(String name) {
        return resolve(name).orElseThrow(() -> new NoSuchElementException(name
                + " not found"));
    }

    default boolean hasName(String name) {
        return resolve(name).isPresent();
    }

    default void ifResolvedOrElse(String name, Consumer<? super T> action,
            Runnable emptyAction) {
        if (hasName(name)) {
            action.accept(resolveOrThrow(name));
        } else {
            emptyAction.run();
        }
    }
}
