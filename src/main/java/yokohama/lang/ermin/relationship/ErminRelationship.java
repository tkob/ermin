package yokohama.lang.ermin.relationship;

import java.util.Optional;
import java.util.function.BiFunction;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminRelationship {
    private final ErminName name;

    private final ErminRelationshipExp exp;

    public int getArity() {
        return exp.getArity();
    }

    public <R> Optional<R> applyBiFunction(
            BiFunction<ErminAtomicRelationshipExp, ErminAtomicRelationshipExp, R> f) {

        return this.getExp().accept(new ErminRelationshipExpVisitor<Optional<R>>() {

            @Override
            public Optional<R> visitAtomicRelationshipExp(
                    ErminAtomicRelationshipExp atomicRelationshipExp) {

                // unary relationship
                return Optional.empty();
            }

            @Override
            public Optional<R> visitProductRelationshipExp(
                    ErminProductRelationshipExp productRelationshipExp) {

                return productRelationshipExp.getLeft().accept(
                        new ErminRelationshipExpVisitor<Optional<R>>() {

                            @Override
                            public Optional<R> visitAtomicRelationshipExp(
                                    ErminAtomicRelationshipExp left) {

                                return productRelationshipExp.getRight().accept(
                                        new ErminRelationshipExpVisitor<Optional<R>>() {

                                            @Override
                                            public Optional<R> visitAtomicRelationshipExp(
                                                    ErminAtomicRelationshipExp right) {

                                                // binary relationship
                                                return Optional.ofNullable(f.apply(left, right));
                                            }

                                            @Override
                                            public Optional<R> visitProductRelationshipExp(
                                                    ErminProductRelationshipExp productRelationshipExp) {

                                                // multi relationship
                                                return Optional.empty();
                                            }
                                        });
                            }

                            @Override
                            public Optional<R> visitProductRelationshipExp(
                                    ErminProductRelationshipExp productRelationshipExp) {

                                // multi relationship
                                return Optional.empty();
                            }
                        });
            }
        });
    }
}
