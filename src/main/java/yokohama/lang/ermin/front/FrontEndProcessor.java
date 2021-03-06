package yokohama.lang.ermin.front;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import yokohama.lang.ermin.Absyn.BoolExp;
import yokohama.lang.ermin.Absyn.ChildEntityDef;
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.Decl;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.DefaultRelationshipType;
import yokohama.lang.ermin.Absyn.DeleteStatement;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.EqualBoolExp;
import yokohama.lang.ermin.Absyn.ExistingEntityArgument;
import yokohama.lang.ermin.Absyn.Exp;
import yokohama.lang.ermin.Absyn.GuardedProcessDef;
import yokohama.lang.ermin.Absyn.InDecl;
import yokohama.lang.ermin.Absyn.InsertStatement;
import yokohama.lang.ermin.Absyn.KeyOnlyChildEntityDef;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.ListAttribute;
import yokohama.lang.ermin.Absyn.ListBoolExp;
import yokohama.lang.ermin.Absyn.ListIdent;
import yokohama.lang.ermin.Absyn.NewEntityArgument;
import yokohama.lang.ermin.Absyn.NotEqualBoolExp;
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
import yokohama.lang.ermin.Absyn.TupleExp;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.Absyn.UniversalBoolExp;
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
import yokohama.lang.ermin.process.ErminBoolExp;
import yokohama.lang.ermin.process.ErminDecl;
import yokohama.lang.ermin.process.ErminDeleteStatement;
import yokohama.lang.ermin.process.ErminEqualBoolExp;
import yokohama.lang.ermin.process.ErminExistingEntityArgument;
import yokohama.lang.ermin.process.ErminExp;
import yokohama.lang.ermin.process.ErminInsertStatement;
import yokohama.lang.ermin.process.ErminNewEntityArgument;
import yokohama.lang.ermin.process.ErminNotEqualBoolExp;
import yokohama.lang.ermin.process.ErminStatement;
import yokohama.lang.ermin.process.ErminTupleExp;
import yokohama.lang.ermin.process.ErminUniversalBoolExp;
import yokohama.lang.ermin.process.ErminUpdateStatement;
import yokohama.lang.ermin.process.ErminVarExp;
import yokohama.lang.ermin.relationship.ErminBinaryRelationship;
import yokohama.lang.ermin.relationship.ErminMultiRelationship;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminRelationship;
import yokohama.lang.ermin.relationship.ErminRelationshipExp;

public class FrontEndProcessor {

    AbsynAttributeToErminAttribute absynAttributeToErminAttribute = new AbsynAttributeToErminAttribute();

    AbsynTypeToErminType absynTypeToErminType = new AbsynTypeToErminType();

    CodeResolverFactory codeResolverFactory = new CodeResolverFactory();

    TypeResolverFactory typeResolverFactory = new TypeResolverFactory();

    public ErminTuple process(InputStream is) throws Exception {
        final Yylex l = new Yylex(new InputStreamReader(is));
        final parser p = new parser(l);
        try {
            return process(p.pTop());
        } catch (Throwable e) {
            throw new RuntimeException("syntax error at line " + l.line_num(), e);
        }
    }

    public ErminTuple process(Reader reader) throws Exception {
        final Yylex l = new Yylex(reader);
        final parser p = new parser(l);
        try {
            return process(p.pTop());
        } catch (Throwable e) {
            throw new RuntimeException("syntax error at line " + l.line_num(), e);
        }
    }

    public ErminTuple process(Top top) {
        final CodeResolver codeResolver = codeResolverFactory.fromAbsyn(top);
        final TypeResolver typeResolver = typeResolverFactory.fromAbsyn(top, codeResolver);

        final Map<ErminName, ErminEntity> nameToEntity =
            top.accept(new Top.Visitor<Map<ErminName, ErminEntity>, TypeResolver>() {
                @Override
                public Map<ErminName, ErminEntity> visit(TopDefinitions p, TypeResolver typeResolver) {
                    final List<ChildEntityDef> entityDefs =
                        filterEntityDef(p.listdef_.stream()).collect(Collectors.toList());

                    final List<ErminName> entityNames =
                        entityDefs.stream()
                                  .map(entityDef -> ErminName.fromSnake(entityDef.ident_1))
                                  .collect(Collectors.toList());

                    final Map<ErminName, ErminEntity> nameToEntity = new HashMap<>();
                    entityDefs.forEach(entityDef -> {
                        final ErminEntity entity = toErminEntity(entityDef, typeResolver, entityNames);
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
                public Collection<ErminRelationship> visit(TopDefinitions p, TypeResolver arg) {
                    return filterRelationshipDef(p.listdef_.stream()).map(relationshipDef -> toErminRelationship(relationshipDef))
                                                                     .collect(Collectors.toList());
                }
            }, typeResolver);

        final Collection<ErminAbstractProcess> abstractProcesses =
            top.accept(new Top.Visitor<Collection<ErminAbstractProcess>, TypeResolver>() {

                @Override
                public Collection<ErminAbstractProcess> visit(TopDefinitions p, TypeResolver arg) {
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

    public ErminEntity toErminEntity(ChildEntityDef entityDef, TypeResolver typeResolver,
            Collection<ErminName> entityNames) {
        final ErminName entityName = ErminName.fromSnake(entityDef.ident_1);

        final List<ErminName> entityKeys =
            entityDef.listident_.stream().map(ErminName::fromSnake).map(parentName -> {
                if (entityNames.contains(parentName)) {
                    return parentName;
                } else {
                    throw new RuntimeException(parentName + " is not an entity name");
                }
            }).collect(Collectors.toList());

        final ErminName identifierName = ErminName.fromSnake(entityDef.ident_2);
        final ErminKey identifierKey =
            new ErminKey(identifierName, entityDef.type_.accept(absynTypeToErminType, typeResolver));

        final List<ErminAttribute> attributes =
            entityDef.listattribute_.stream()
                                    .map(attribute -> attribute.accept(absynAttributeToErminAttribute,
                                                                       typeResolver))
                                    .collect(Collectors.toList());

        return new ErminEntity(entityName, identifierKey, entityKeys, attributes);
    }

    public ErminRelationship toErminRelationship(RelationshipDef relationshipDef) {
        final ErminName name = ErminName.fromSnake(relationshipDef.ident_);
        final List<ErminRelationshipExp> exps =
            relationshipDef.listrelationshiptype_.stream()
                                                 .map(this::toErminRelationshipExp)

                                                 .collect(Collectors.toList());
        if (exps.size() == 1) {
            return new ErminMultiRelationship(name,
                                              exps.stream()
                                                  .map(ErminRelationshipExp::getName)
                                                  .collect(Collectors.toList()));
        } else if (exps.size() == 2) {
            return new ErminBinaryRelationship(name, exps.get(0), exps.get(1));
        } else {
            if (exps.stream().anyMatch(exp -> exp.getMultiplicity() != ErminMultiplicity.ZERO_OR_MORE)) {
                throw new RuntimeException("multirelation cannot have multiplicity other than zero or more");
            }
            return new ErminMultiRelationship(name,
                                              exps.stream()
                                                  .map(ErminRelationshipExp::getName)
                                                  .collect(Collectors.toList()));
        }
    }

    public ErminRelationshipExp toErminRelationshipExp(RelationshipType relationshiptype_) {
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

    public ErminDecl toErminDecl(InDecl decl) {
        return new ErminDecl(ErminName.fromSnake(decl.ident_1), ErminName.fromSnake(decl.ident_2));
    }

    public ErminBoolExp toErminBoolExp(BoolExp boolExp) {
        return boolExp.accept(new BoolExp.Visitor<ErminBoolExp, Void>() {

            @Override
            public ErminBoolExp visit(UniversalBoolExp p, Void arg) {
                return new ErminUniversalBoolExp(p.listdecl_.stream()
                                                            .map(decl -> decl.accept(new Decl.Visitor<ErminDecl, Void>() {
                                                                @Override
                                                                public ErminDecl visit(InDecl p, Void arg) {
                                                                    return toErminDecl(p);
                                                                }
                                                            }, null))
                                                            .collect(Collectors.toList()),
                                                 toErminBoolExp(p.boolexp_));
            }

            @Override
            public ErminBoolExp visit(EqualBoolExp p, Void arg) {
                return new ErminEqualBoolExp(toErminExp(p.exp_1), toErminExp(p.exp_2));
            }

            @Override
            public ErminBoolExp visit(NotEqualBoolExp p, Void arg) {
                return new ErminNotEqualBoolExp(toErminExp(p.exp_1), toErminExp(p.exp_2));
            }
        }, null);
    }

    public ErminAbstractProcess toErminAbstractProcess(GuardedProcessDef abstractProcessDef) {
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
        final List<ErminBoolExp> guards =
            abstractProcessDef.listboolexp_.stream().map(this::toErminBoolExp).collect(Collectors.toList());
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

        return new ErminAbstractProcess(name, arguments, guards, statements);
    }

    public ErminExp toErminExp(Exp exp) {
        return exp.accept(new Exp.Visitor<ErminExp, Void>() {

            @Override
            public ErminExp visit(VarExp p, Void arg) {
                return new ErminVarExp(p.ident_);
            }

            @Override
            public ErminExp visit(TupleExp p, Void arg) {
                return new ErminTupleExp(p.listexp_.stream().map(exp -> {
                    return toErminExp(exp);
                }).collect(Collectors.toList()));
            }

        }, null);
    }

    public Stream<ChildEntityDef> filterEntityDef(Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<ChildEntityDef>, Void>() {

            @Override
            public Stream<ChildEntityDef> visit(TypeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<ChildEntityDef> visit(CodeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<ChildEntityDef> visit(EntityDef p, Void arg) {
                return Stream.of(new ChildEntityDef(p.ident_1,
                                                    new ListIdent(),
                                                    p.ident_2,
                                                    p.type_,
                                                    p.listattribute_));
            }

            @Override
            public Stream<ChildEntityDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.of(new ChildEntityDef(p.ident_1,
                                                    new ListIdent(),
                                                    p.ident_2,
                                                    p.type_,
                                                    new ListAttribute()));
            }

            @Override
            public Stream<ChildEntityDef> visit(ChildEntityDef p, Void arg) {
                return Stream.of(p);
            }

            @Override
            public Stream<ChildEntityDef> visit(KeyOnlyChildEntityDef p, Void arg) {
                return Stream.of(new ChildEntityDef(p.ident_1,
                                                    p.listident_,
                                                    p.ident_2,
                                                    p.type_,
                                                    new ListAttribute()));
            }

            @Override
            public Stream<ChildEntityDef> visit(RelationshipDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<ChildEntityDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<ChildEntityDef> visit(GuardedProcessDef p, Void arg) {
                return Stream.empty();
            }
        }, null));
    }

    public Stream<RelationshipDef> filterRelationshipDef(Stream<Def> defs) {
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
            public Stream<RelationshipDef> visit(EntityDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(ChildEntityDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

            @Override
            public Stream<RelationshipDef> visit(KeyOnlyChildEntityDef p, Void arg) {
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

            @Override
            public Stream<RelationshipDef> visit(GuardedProcessDef p, Void arg) {
                return Stream.<RelationshipDef> empty();
            }

        }, null));
    }

    public Stream<GuardedProcessDef> filterAbstractProcessDef(Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(new Def.Visitor<Stream<GuardedProcessDef>, Void>() {

            @Override
            public Stream<GuardedProcessDef> visit(TypeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(CodeDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(ChildEntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(KeyOnlyChildEntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(EntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(KeyOnlyEntityDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(RelationshipDef p, Void arg) {
                return Stream.empty();
            }

            @Override
            public Stream<GuardedProcessDef> visit(AbstractProcessDef p, Void arg) {
                return Stream.of(new GuardedProcessDef(p.ident_,
                                                       p.listargument_,
                                                       new ListBoolExp(),
                                                       p.liststatement_));
            }

            @Override
            public Stream<GuardedProcessDef> visit(GuardedProcessDef p, Void arg) {
                return Stream.of(p);
            }

        }, null));
    }
}
