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
import yokohama.lang.ermin.Absyn.BlobType;
import yokohama.lang.ermin.Absyn.CharType;
import yokohama.lang.ermin.Absyn.ChildEntityDef;
import yokohama.lang.ermin.Absyn.ClobType;
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.DateType;
import yokohama.lang.ermin.Absyn.DecimalPrecisionScaleType;
import yokohama.lang.ermin.Absyn.DecimalPrecisionType;
import yokohama.lang.ermin.Absyn.DecimalType;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.IdentType;
import yokohama.lang.ermin.Absyn.IntegerType;
import yokohama.lang.ermin.Absyn.KeyOnlyChildEntityDef;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.Type;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.Absyn.VarCharType;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.type.ErminBlobType;
import yokohama.lang.ermin.type.ErminCharType;
import yokohama.lang.ermin.type.ErminClobType;
import yokohama.lang.ermin.type.ErminDateType;
import yokohama.lang.ermin.type.ErminDecimalType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminStringCodeType;
import yokohama.lang.ermin.type.ErminType;
import yokohama.lang.ermin.type.ErminVarCharType;

@RequiredArgsConstructor
public class TypeResolverFactory {

    public TypeResolver fromAbsyn(Top top, CodeResolver codeResolver) {
        return top.accept(new Top.Visitor<TypeResolver, CodeResolver>() {

            @Override
            public TypeResolver visit(TopDefinitions p, CodeResolver codeResolver) {
                final Stream<TypeDef> typeDefs = filterTypeDef(p.listdef_.stream());
                return fromTypeDefs(typeDefs.collect(Collectors.toList()), codeResolver);
            }
        }, codeResolver);
    }

    public TypeResolver fromTypeDefs(Iterable<TypeDef> typeDefs, CodeResolver codeResolver) {
        final Set<ErminName> typeNames = new HashSet<>();
        codeResolver.getNames().forEach(name -> {
            if (typeNames.contains(name)) {
                throw new RuntimeException("duplicate code definition: " + name);
            } else {
                typeNames.add(name);
            }
        });
        typeDefs.forEach(typeDef -> {
            final ErminName typeName = ErminName.fromSnake(typeDef.ident_);
            if (typeNames.contains(typeName)) {
                throw new RuntimeException("duplicate type/code definition: " + typeName);
            } else {
                typeNames.add(typeName);
            }
        });

        final Map<ErminName, ErminType> nameToType = new HashMap<>();
        final Map<ErminName, ErminName> nameToName = new HashMap<>();

        codeResolver.getNames().forEach(name -> {
            nameToType.put(name, new ErminStringCodeType(name));
        });
        typeDefs.forEach(typeDef -> {
            final ErminName typeName = ErminName.fromSnake(typeDef.ident_);
            typeDef.type_.accept(new Type.Visitor<Void, TypeDef>() {

                @Override
                public Void visit(CharType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminCharType(p.integer_));
                    return null;
                }

                @Override
                public Void visit(VarCharType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminVarCharType(p.integer_));
                    return null;
                }

                @Override
                public Void visit(ClobType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminClobType());
                    return null;
                }

                @Override
                public Void visit(BlobType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminBlobType());
                    return null;
                }

                @Override
                public Void visit(DecimalType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminDecimalType());
                    return null;
                }

                @Override
                public Void visit(DecimalPrecisionType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminDecimalType(p.integer_));
                    return null;
                }

                @Override
                public Void visit(DecimalPrecisionScaleType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminDecimalType(p.integer_1, p.integer_2));
                    return null;
                }

                @Override
                public Void visit(IntegerType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminIntegerType());
                    return null;
                }

                @Override
                public Void visit(DateType p, TypeDef typeDef) {
                    nameToType.put(typeName, new ErminDateType());
                    return null;
                }

                @Override
                public Void visit(IdentType p, TypeDef typeDef) {
                    final ErminName name = ErminName.fromSnake(p.ident_);
                    if (codeResolver.hasName(name)) {
                        nameToType.put(typeName, new ErminStringCodeType(name));
                    } else {
                        // cannot resolve to conrete type yet
                        nameToName.put(typeName, name);
                    }
                    return null;
                }
            }, typeDef);
        });

        for (ErminName fromName : nameToName.keySet()) {
            ErminName toName = nameToName.get(fromName);
            final Set<ErminName> seen = new HashSet<>();
            seen.add(fromName);
            while (true) {
                if (nameToType.containsKey(toName)) {
                    nameToType.put(fromName, nameToType.get(toName));
                    break;
                } else if (nameToName.containsKey(toName)) {
                    if (seen.contains(toName)) {
                        throw new RuntimeException("cyclic type definition beginning from " + fromName);
                    }
                    seen.add(toName);
                    toName = nameToName.get(toName);
                } else {
                    throw new RuntimeException("type name " + toName + " not defined");
                }

            }

        }

        return new TypeResolver() {

            @Override
            public Optional<ErminType> resolve(ErminName name) {
                return Optional.ofNullable(nameToType.get(name));

            }
        };

    }

    public Stream<TypeDef> filterTypeDef(Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<TypeDef>, Void>() {

            @Override
            public Stream<TypeDef> visit(TypeDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<TypeDef> visit(CodeDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(EntityDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(ChildEntityDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(KeyOnlyChildEntityDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(RelationshipDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }

            @Override
            public Stream<TypeDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.<TypeDef> empty();
            }
        }, null));
    }

}
