package yokohama.lang.ermin.front;

import java.util.Collection;

import lombok.Value;
import yokohama.lang.ermin.entity.ErminEntity;

@Value
public class ErminTuple {
    private final CodeResolver codeResolver;

    private final TypeResolver typeResolver;

    private final Collection<ErminEntity> entities;
}
