package yokohama.lang.ermin.relationship;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminRelationship {
    private final ErminName name;

    private final ErminRelationshipExp exp;

    public int getArity() {
        return exp.getArity();
    }
}
