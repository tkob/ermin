package yokohama.lang.ermin.front;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import yokohama.lang.ermin.Absyn.AbstractProcessDef;
import yokohama.lang.ermin.Absyn.CharIdType;
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.IdentifierDef;
import yokohama.lang.ermin.Absyn.IdentifierType;
import yokohama.lang.ermin.Absyn.IntegerIdType;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.Absyn.VarCharIdType;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.type.ErminCharType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminType;
import yokohama.lang.ermin.type.ErminVarCharType;

@RequiredArgsConstructor
public class IdentifierResolverFactory {

    public TypeResolver fromAbsyn(Top top) {
        return top.accept(new Top.Visitor<TypeResolver, Void>() {

            @Override
            public TypeResolver visit(TopDefinitions p, Void arg) {
                Stream<IdentifierDef> identifierDefs = filterIdentifierDef(p.listdef_.stream());
                return fromIdentifierDefs(identifierDefs.collect(Collectors.toList()));
            }
        }, null);
    }

    public TypeResolver fromIdentifierDefs(Iterable<IdentifierDef> identifierDefs) {
        Set<String> identifierNames = new HashSet<String>();

        identifierDefs.forEach(identifierDef -> {
            String identifierName = identifierDef.ident_;
            if (identifierNames.contains(identifierName)) {
                throw new RuntimeException("duplicate identifier definition: " + identifierName);
            } else {
                identifierNames.add(identifierName);
            }
        });

        Map<ErminName, ErminType> nameToType = new HashMap<>();

        identifierDefs.forEach(identifierDef -> {
            identifierDef.identifiertype_.accept(new IdentifierType.Visitor<Void, IdentifierDef>() {

                @Override
                public Void visit(CharIdType p, IdentifierDef arg) {
                    nameToType.put(ErminName.fromSnake(arg.ident_), new ErminCharType(p.integer_));
                    return null;
                }

                @Override
                public Void visit(VarCharIdType p, IdentifierDef arg) {
                    nameToType.put(ErminName.fromSnake(arg.ident_), new ErminVarCharType(p.integer_));
                    return null;
                }

                @Override
                public Void visit(IntegerIdType p, IdentifierDef arg) {
                    nameToType.put(ErminName.fromSnake(arg.ident_), new ErminIntegerType());
                    return null;
                }
            }, identifierDef);
        });

        return new TypeResolver() {

            @Override
            public Optional<ErminType> resolve(ErminName name) {
                return Optional.ofNullable(nameToType.get(name));

            }
        };

    }

    public Stream<IdentifierDef> filterIdentifierDef(final Stream<Def> defs) {
        return defs.flatMap(def -> def.accept(new Def.Visitor<Stream<IdentifierDef>, Void>() {

            @Override
            public Stream<IdentifierDef> visit(TypeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<IdentifierDef> visit(CodeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<IdentifierDef> visit(IdentifierDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<IdentifierDef> visit(EntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<IdentifierDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<IdentifierDef> visit(RelationshipDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<IdentifierDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.empty();
            }
        }, null));
    }

}
