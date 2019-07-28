package yokohama.lang.ermin.front;

import java.util.Optional;

public interface CodeResolver {
    Optional<Iterable<String>> resolve(String name);

    Iterable<String> resolveOrThrow(String name);

    Iterable<String> getNames();

    boolean hasName(String name);
}
