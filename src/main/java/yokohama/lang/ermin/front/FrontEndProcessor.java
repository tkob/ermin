package yokohama.lang.ermin.front;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import yokohama.lang.ermin.Yylex;
import yokohama.lang.ermin.parser;
import yokohama.lang.ermin.Absyn.AbstractProcessDef;
import yokohama.lang.ermin.Absyn.Argument;
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.DefaultRelationshipType;
import yokohama.lang.ermin.Absyn.DeleteStatement;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.ExistingEntityArgument;
import yokohama.lang.ermin.Absyn.Exp;
import yokohama.lang.ermin.Absyn.IdentifierDef;
import yokohama.lang.ermin.Absyn.InsertStatement;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.ListAttribute;
import yokohama.lang.ermin.Absyn.NewEntityArgument;
import yokohama.lang.ermin.Absyn.NumericOneOreMoreRelationshipType;
import yokohama.lang.ermin.Absyn.NumericOneRelationshipType;
import yokohama.lang.ermin.Absyn.NumericZeroOrMoreRelationshipType;
import yokohama.lang.ermin.Absyn.NumericZeroOrOneRelationshipType;
import yokohama.lang.ermin.Absyn.OneOreMoreRelationshipType;
import yokohama.lang.ermin.Absyn.OneRelationshipType;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.RelationshipType;
import yokohama.lang.ermin.Absyn.Statement;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.Absyn.UpdateStatement;
import yokohama.lang.ermin.Absyn.VarExp;
import yokohama.lang.ermin.Absyn.ZeroOrMoreRelationshipType;
import yokohama.lang.ermin.Absyn.ZeroOrOneRelationshipType;
import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminKey;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.process.ErminAbstractProcess;
import yokohama.lang.ermin.process.ErminArgument;
import yokohama.lang.ermin.process.ErminDeleteStatement;
import yokohama.lang.ermin.process.ErminExistingEntityArgument;
import yokohama.lang.ermin.process.ErminExp;
import yokohama.lang.ermin.process.ErminInsertStatement;
import yokohama.lang.ermin.process.ErminNewEntityArgument;
import yokohama.lang.ermin.process.ErminStatement;
import yokohama.lang.ermin.process.ErminUpdateStatement;
import yokohama.lang.ermin.process.ErminVarExp;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminRelationship;
import yokohama.lang.ermin.relationship.ErminRelationshipExp;

public class FrontEndProcessor {

    AbsynAttributeToErminAttribute absynAttributeToErminAttribute = new AbsynAttributeToErminAttribute();

    AbsynTypeToErminType absynTypeToErminType = new AbsynTypeToErminType();

    CodeResolverFactory codeResolverFactory = new CodeResolverFactory();

    TypeResolverFactory typeResolverFactory = new TypeResolverFactory();

    IdentifierResolverFactory identifierResolverFactory = new IdentifierResolverFactory();

    public ErminTuple process(InputStream is) throws Exception {
        final Yylex l = new Yylex(new InputStreamReader(is));
        final parser p = new parser(l);
        return process(p.pTop());
    }

    public ErminTuple process(Reader reader) throws Exception {
        final Yylex l = new Yylex(reader);
        final parser p = new parser(l);
        return process(p.pTop());
    }

    public ErminTuple process(Top top) {
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);
        final TypeResolver typeResolver = typeResolverFactory.fromAbsyn(top, codeResolver);
        final TypeResolver identifierResolver = identifierResolverFactory.fromAbsyn(top);

        final Map<ErminName, ErminEntity> nameToEntity =
            top.accept(new Top.Visitor<Map<ErminName, ErminEntity>, TypeResolver>() {
                @Override
                public Map<ErminName, ErminEntity> visit(final TopDefinitions p,
                        final TypeResolver typeResolver) {
                    List<EntityDef> entityDefs =
                        filterEntityDef(p.listdef_.stream()).collect(Collectors.toList());

                    List<ErminName> entityNames =
                        entityDefs.stream()
                                  .map(entityDef -> ErminName.fromSnake(entityDef.ident_))
                                  .collect(Collectors.toList());

                    Map<ErminName, ErminEntity> nameToEntity = new HashMap<>();
                    entityDefs.forEach(entityDef -> {
                        ErminEntity entity =
                            toErminEntity(entityDef, typeResolver, identifierResolver, entityNames);
                        nameToEntity.put(entity.getName(), entity);
                    });
                    return nameToEntity;
                }
            }, typeResolver);

        final Resolver<ErminName, ErminEntity> entityResolver = new Resolver<ErminName, ErminEntity>() {

            @Override
            public Optional<ErminEntity> resolve(ErminName name) {
                return Optional.ofNullable(nameToEntity.get(name));
            }
        };

        final Collection<ErminEntity> entities = nameToEntity.values();

        final Collection<ErminRelationship> relationships =
            top.accept(new Top.Visitor<Collection<ErminRelationship>, TypeResolver>() {

                @Override
                public Collection<ErminRelationship> visit(final TopDefinitions p, final TypeResolver arg) {
                    return filterRelationshipDef(p.listdef_.stream()).map(relationshipDef -> toErminRelationship(relationshipDef))
                                                                     .collect(Collectors.toList());
                }
            }, typeResolver);

        final Collection<ErminAbstractProcess> abstractProcesses =
            top.accept(new Top.Visitor<Collection<ErminAbstractProcess>, TypeResolver>() {

                @Override
                public Collection<ErminAbstractProcess> visit(final TopDefinitions p, TypeResolver arg) {
                    return filterAbstractProcessDef(p.listdef_.stream()).map(abstractProcessDef -> toErminAbstractProcess(abstractProcessDef))
                                                                        .collect(Collectors.toList());
                }
            }, typeResolver);

        return new ErminTuple(codeResolver,
                              typeResolver,
                              entityResolver,
                              entities,
                              relationships,
                              abstractProcesses);
    }

    public ErminEntity toErminEntity(EntityDef entityDef, TypeResolver typeResolver,
            TypeResolver identifierResolver, Collection<ErminName> entityNames) {
        ErminName entityName = ErminName.fromSnake(entityDef.ident_);

        final List<ErminKey> identifierKeys = new ArrayList<>();
        final List<ErminName> entityKeys = new ArrayList<>();
        for (String keyRef : entityDef.listident_) {
            ErminName name = ErminName.fromSnake(keyRef);
            identifierResolver.ifResolvedOrElse(name, (type -> {
                identifierKeys.add(new ErminKey(name, type));
            }), () -> {
                if (entityNames.contains(name)) {
                    entityKeys.add(name);
                } else {
                    throw new RuntimeException();
                }
            });
        }

        Optional<ErminKey> identifierKey;
        if (identifierKeys.isEmpty()) {
            identifierKey = Optional.empty();
        } else if (identifierKeys.size() == 1) {
            identifierKey = Optional.of(identifierKeys.get(0));
        } else {
            throw new RuntimeException();
        }

        List<ErminAttribute> attributes =
            entityDef.listattribute_.stream()
                                    .map(attribute -> attribute.accept(absynAttributeToErminAttribute,
                                                                       typeResolver))
                                    .collect(Collectors.toList());

        return new ErminEntity(entityName, identifierKey, entityKeys, attributes);
    }

    public ErminRelationship toErminRelationship(final RelationshipDef relationshipDef) {
        ErminName name = ErminName.fromSnake(relationshipDef.ident_);
        List<ErminRelationshipExp> exps =
            relationshipDef.listrelationshiptype_.stream()
                                                 .map(this::toErminRelationshipExp)
                                                 .collect(Collectors.toList());
        if (exps.size() >= 3
                && exps.stream().anyMatch(exp -> exp.getMultiplicity() != ErminMultiplicity.ZERO_OR_MORE)) {
            throw new RuntimeException("multirelation cannot have multiplicity other than zero or more");
        }
        return new ErminRelationship(name, exps);
    }

    public ErminRelationshipExp toErminRelationshipExp(final RelationshipType relationshiptype_) {
        return relationshiptype_.accept(new RelationshipType.Visitor<ErminRelationshipExp, Void>() {

            @Override
            public ErminRelationshipExp visit(OneRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ONE, ErminName.fromSnake(p.ident_));
            }

            @Override
            public ErminRelationshipExp visit(ZeroOrOneRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ZERO_OR_ONE, ErminName.fromSnake(p.ident_));
            }

            @Override
            public ErminRelationshipExp visit(ZeroOrMoreRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ZERO_OR_MORE,
                                                ErminName.fromSnake(p.ident_));
            }

            @Override
            public ErminRelationshipExp visit(OneOreMoreRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ONE_OR_MORE, ErminName.fromSnake(p.ident_));
            }

            @Override
            public ErminRelationshipExp visit(NumericOneRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ONE, ErminName.fromSnake(p.ident_));

            }

            @Override
            public ErminRelationshipExp visit(NumericZeroOrOneRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ZERO_OR_ONE, ErminName.fromSnake(p.ident_));

            }

            @Override
            public ErminRelationshipExp visit(NumericZeroOrMoreRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ZERO_OR_MORE,
                                                ErminName.fromSnake(p.ident_));

            }

            @Override
            public ErminRelationshipExp visit(NumericOneOreMoreRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ONE_OR_MORE, ErminName.fromSnake(p.ident_));

            }

            @Override
            public ErminRelationshipExp visit(DefaultRelationshipType p, Void arg) {
                return new ErminRelationshipExp(ErminMultiplicity.ZERO_OR_MORE,
                                                ErminName.fromSnake(p.ident_));

            }
        }, null);
    }

    public ErminAbstractProcess toErminAbstractProcess(final AbstractProcessDef abstractProcessDef) {
        final ErminName name = ErminName.fromSnake(abstractProcessDef.ident_);

        final List<ErminArgument> arguments =
            abstractProcessDef.listargument_.stream()
                                            .map(argument -> argument.accept(new Argument.Visitor<ErminArgument, Void>() {

                                                @Override
                                                public ErminArgument visit(NewEntityArgument p, Void arg) {
                                                    return new ErminNewEntityArgument(p.ident_1,
                                                                                      ErminName.fromSnake(p.ident_2));
                                                }

                                                @Override
                                                public ErminArgument visit(ExistingEntityArgument p,
                                                        Void arg) {
                                                    return new ErminExistingEntityArgument(p.ident_1,
                                                                                           ErminName.fromSnake(p.ident_2));
                                                }
                                            }, null))
                                            .collect(Collectors.toList());

        final List<ErminStatement> statements =
            abstractProcessDef.liststatement_.stream()
                                             .map(statement -> statement.accept(new Statement.Visitor<ErminStatement, Void>() {

                                                 @Override
                                                 public ErminStatement visit(InsertStatement p, Void arg) {
                                                     return new ErminInsertStatement(ErminName.fromSnake(p.ident_),
                                                                                     toErminExp(p.exp_));
                                                 }

                                                 @Override
                                                 public ErminStatement visit(DeleteStatement p, Void arg) {
                                                     return new ErminDeleteStatement(ErminName.fromSnake(p.ident_),
                                                                                     toErminExp(p.exp_));
                                                 }

                                                 @Override
                                                 public ErminStatement visit(UpdateStatement p, Void arg) {
                                                     return new ErminUpdateStatement(ErminName.fromSnake(p.ident_),
                                                                                     p.listexp_.stream()
                                                                                               .map(exp -> toErminExp(exp))
                                                                                               .collect(Collectors.toList()));
                                                 }
                                             }, null))
                                             .collect(Collectors.toList());

        return new ErminAbstractProcess(name, arguments, statements);
    }

    public ErminExp toErminExp(Exp exp) {
        return exp.accept(new Exp.Visitor<ErminExp, Void>() {

            @Override
            public ErminExp visit(VarExp p, Void arg) {
                return new ErminVarExp(p.ident_);
            }
        }, null);
    }

    public Stream<EntityDef> filterEntityDef(final Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<EntityDef>, Void>() {

            @Override
            public Stream<EntityDef> visit(TypeDef p, Void arg) {
                return Stream.<EntityDef> empty();
            }

            @Override
            public Stream<EntityDef> visit(CodeDef p, Void arg) {
                return Stream.<EntityDef> empty();
            }

            @Override
            public Stream<EntityDef> visit(IdentifierDef p, Void arg) {
                return Stream.<EntityDef> empty();
            }

            @Override
            public Stream<EntityDef> visit(EntityDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<EntityDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.of(new EntityDef(p.ident_, p.listident_, new ListAttribute()));
            }

            @Override
            public Stream<EntityDef> visit(RelationshipDef p, Void arg) {
                return Stream.<EntityDef> empty();
            }

            @Override
            public Stream<EntityDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.<EntityDef> empty();
            }
        }, null));
    }

    public Stream<RelationshipDef> filterRelationshipDef(final Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<RelationshipDef>, Void>() {

            @Override
            public Stream<RelationshipDef> visit(TypeDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(CodeDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(IdentifierDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(EntityDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(RelationshipDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<RelationshipDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }
        }, null));
    }

    public Stream<AbstractProcessDef> filterAbstractProcessDef(final Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<AbstractProcessDef>, Void>() {

            @Override
            public Stream<AbstractProcessDef> visit(TypeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(CodeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(IdentifierDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(EntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(RelationshipDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<AbstractProcessDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.of(p);
            }
        }, null));
    }
}
