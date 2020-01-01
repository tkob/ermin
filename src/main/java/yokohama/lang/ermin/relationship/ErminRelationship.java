package yokohama.lang.ermin.relationship;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import yokohama.lang.ermin.attribute.ErminName;

public interface ErminRelationship {
    ErminName getName();

    List<ErminName> getEntityNames();

    <R> Optional<R> applyBiFunction(BiFunction<ErminRelationshipExp, ErminRelationshipExp, R> f);

    <R> R accept(ErminRelationshipVisitor<R> visitor);
}
