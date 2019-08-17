package yokohama.lang.ermin.relationship;

import lombok.Value;

@Value
public class ErminProductRelationshipExp implements ErminRelationshipExp {

    private final ErminRelationshipExp left;

    private final ErminRelationshipExp right;

    @Override
    public <R> R accept(ErminRelationshipExpVisitor<R> visitor) {
        return visitor.visitProductRelationshipExp(this);
    }

    @Override
    public int getArity() {
        return left.getArity() + right.getArity();
    }
}
