package yokohama.lang.ermin.relationship;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminAtomicRelationshipExp implements ErminRelationshipExp {

    private final ErminMultiplicity multiplicity;

    private final ErminName name;

    @Override
    public <R> R accept(ErminRelationshipExpVisitor<R> visitor) {
        return visitor.visitAtomicRelationshipExp(this);
    }
}
