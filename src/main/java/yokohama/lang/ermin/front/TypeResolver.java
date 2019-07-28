package yokohama.lang.ermin.front;

import java.util.Optional;

import yokohama.lang.ermin.type.ErminType;

public interface TypeResolver {
    Optional<ErminType> resolve(String name);

    ErminType resolveOrThrow(String name);
}
