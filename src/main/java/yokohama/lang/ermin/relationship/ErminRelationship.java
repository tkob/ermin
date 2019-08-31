package yokohama.lang.ermin.relationship;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminRelationship {
    private final ErminName name;

    private final List<ErminRelationshipExp> exps;

    public <R> Optional<R> applyBiFunction(
            BiFunction<ErminRelationshipExp, ErminRelationshipExp, R> f) {
        if (exps.size() == 2) {
            return Optional.ofNullable(f.apply(exps.get(0), exps.get(1)));
        } else {
            return Optional.empty();
        }
    }
}
