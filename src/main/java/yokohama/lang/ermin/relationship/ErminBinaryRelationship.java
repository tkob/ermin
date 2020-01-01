package yokohama.lang.ermin.relationship;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminBinaryRelationship implements ErminRelationship {
    private final ErminName name;

    private final ErminRelationshipExp left;

    private final ErminRelationshipExp right;

    @Override
    public List<ErminName> getEntityNames() {
        return Arrays.asList(left.getName(), right.getName());
    }

    @Override
    public <R> Optional<R> applyBiFunction(BiFunction<ErminRelationshipExp, ErminRelationshipExp, R> f) {
        return Optional.ofNullable(f.apply(left, right));
    }

    @Override
    public <R> R accept(ErminRelationshipVisitor<R> visitor) {
        return visitor.visitBinaryRelationship(this);
    }
}
