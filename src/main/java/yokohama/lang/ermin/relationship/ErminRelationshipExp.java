package yokohama.lang.ermin.relationship;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminRelationshipExp {
    private final ErminMultiplicity multiplicity;

    private final ErminName name;
}
