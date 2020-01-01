package yokohama.lang.ermin.relationship;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminMultiRelationship implements ErminRelationship {
    private final ErminName name;

    private final List<ErminName> entityNames;

    @Override
    public <R> Optional<R> applyBiFunction(BiFunction<ErminRelationshipExp, ErminRelationshipExp, R> f) {
        return Optional.empty();
    }

    @Override
    public <R> R accept(ErminRelationshipVisitor<R> visitor) {
        return visitor.visitMultiRelationship(this);
    }
}
