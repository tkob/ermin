package yokohama.lang.ermin.front;

import java.util.Collection;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.process.ErminAbstractProcess;
import yokohama.lang.ermin.relationship.ErminRelationship;

@Value
public class ErminTuple {
    private final CodeResolver codeResolver;

    private final TypeResolver typeResolver;

    private final Resolver<ErminName, ErminEntity> entityResolver;

    private final Collection<ErminEntity> entities;

    private final Collection<ErminRelationship> relationships;

    private final Collection<ErminAbstractProcess> abstractProcesses;
}
