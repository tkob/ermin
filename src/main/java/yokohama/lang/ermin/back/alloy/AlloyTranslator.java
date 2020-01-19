package yokohama.lang.ermin.back.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.back.alloy.ast.AlloyModule;
import yokohama.lang.ermin.back.alloy.ast.ArrowExpr;
import yokohama.lang.ermin.back.alloy.ast.AssertDecl;
import yokohama.lang.ermin.back.alloy.ast.BinOpExpr;
import yokohama.lang.ermin.back.alloy.ast.Block;
import yokohama.lang.ermin.back.alloy.ast.BoxExpr;
import yokohama.lang.ermin.back.alloy.ast.CmdDecl;
import yokohama.lang.ermin.back.alloy.ast.CompareExpr;
import yokohama.lang.ermin.back.alloy.ast.Decl;
import yokohama.lang.ermin.back.alloy.ast.Expr;
import yokohama.lang.ermin.back.alloy.ast.FactDecl;
import yokohama.lang.ermin.back.alloy.ast.LetDecl;
import yokohama.lang.ermin.back.alloy.ast.LetExpr;
import yokohama.lang.ermin.back.alloy.ast.Paragraph;
import yokohama.lang.ermin.back.alloy.ast.PredDecl;
import yokohama.lang.ermin.back.alloy.ast.QualNameExpr;
import yokohama.lang.ermin.back.alloy.ast.Quant;
import yokohama.lang.ermin.back.alloy.ast.QuantExpr;
import yokohama.lang.ermin.back.alloy.ast.Scope;
import yokohama.lang.ermin.back.alloy.ast.SigDecl;
import yokohama.lang.ermin.back.alloy.ast.UnOp;
import yokohama.lang.ermin.back.alloy.ast.UnOpExpr;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.process.ErminAbstractProcess;
import yokohama.lang.ermin.process.ErminArgument;
import yokohama.lang.ermin.process.ErminArgumentVisitor;
import yokohama.lang.ermin.process.ErminBoolExp;
import yokohama.lang.ermin.process.ErminBoolExpVisitor;
import yokohama.lang.ermin.process.ErminDecl;
import yokohama.lang.ermin.process.ErminDeleteStatement;
import yokohama.lang.ermin.process.ErminEqualBoolExp;
import yokohama.lang.ermin.process.ErminExistingEntityArgument;
import yokohama.lang.ermin.process.ErminExp;
import yokohama.lang.ermin.process.ErminExpVisitor;
import yokohama.lang.ermin.process.ErminInsertStatement;
import yokohama.lang.ermin.process.ErminNewEntityArgument;
import yokohama.lang.ermin.process.ErminNotEqualBoolExp;
import yokohama.lang.ermin.process.ErminStatement;
import yokohama.lang.ermin.process.ErminStatementVisitor;
import yokohama.lang.ermin.process.ErminTupleExp;
import yokohama.lang.ermin.process.ErminUniversalBoolExp;
import yokohama.lang.ermin.process.ErminUpdateStatement;
import yokohama.lang.ermin.process.ErminVarExp;
import yokohama.lang.ermin.relationship.ErminBinaryRelationship;
import yokohama.lang.ermin.relationship.ErminMultiRelationship;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminRelationship;
import yokohama.lang.ermin.relationship.ErminRelationshipExp;
import yokohama.lang.ermin.relationship.ErminRelationshipVisitor;

public class AlloyTranslator {

    private final GenSymFactory genSymFactory = new GenSymFactory();

    public AlloyModule toAlloyModule(ErminTuple erminTuple, int scope) {
        final List<Paragraph> paragraphs = new ArrayList<>();
        final GenSym genSym = genSymFactory.create();

        paragraphs.addAll(entitySignatures(erminTuple, genSym));
        paragraphs.addAll(stateSignature(erminTuple, genSym));
        paragraphs.addAll(abstractProcessPredicates(erminTuple, genSym));
        paragraphs.addAll(initPredicate(erminTuple, genSym));
        paragraphs.addAll(tracesFact(erminTuple, genSym));
        paragraphs.addAll(noOrphanEntitiesFact(erminTuple, genSym));
        paragraphs.addAll(multiplicityAssertion(erminTuple, genSym, scope));
        paragraphs.addAll(noDanglingReferenceAssertion(erminTuple, genSym, scope));

        return new AlloyModule(paragraphs);
    }

    /**
     * Create Alloy signatures from entities.
     *
     * @param erminTuple ErminTuple from which the signatures are made
     * @return Signatures
     */
    private Collection<Paragraph> entitySignatures(ErminTuple erminTuple, GenSym genSym) {
        final List<Paragraph> paragraphs = new ArrayList<>();
        for (ErminEntity entity : erminTuple.getEntities()) {
            final SigDecl sigDecl = SigDecl.of(genSym.entityToSigName(entity.getName()));
            paragraphs.add(sigDecl);
        }
        return paragraphs;
    }

    /**
     * Create an Alloy signature which represents global state.
     *
     * @param erminTuple ErminTuple from which the signature is made
     * @return The signature
     */
    private Collection<Paragraph> stateSignature(ErminTuple erminTuple, GenSym genSym) {
        final List<Decl> stateDecls = new ArrayList<>();

        for (ErminEntity entity : erminTuple.getEntities()) {
            final String declName = genSym.entityToDeclName(entity.getName());
            final String sigName = genSym.entityToSigName(entity.getName());
            stateDecls.add(Decl.of(declName, new UnOpExpr(UnOp.SET, QualNameExpr.of(sigName))));
        }

        for (ErminRelationship relationship : erminTuple.getRelationships()) {
            final String name = genSym.relationshipToDeclName(relationship.getName());
            final int arity = relationship.getEntityNames().size();
            final Expr expr;
            if (arity == 1) {
                expr = new UnOpExpr(UnOp.SET,
                                    QualNameExpr.of(genSym.entityToSigName(relationship.getEntityNames()
                                                                                       .get(0))));
            } else {
                final String leftName = genSym.entityToSigName(relationship.getEntityNames().get(0));
                final String rightName = genSym.entityToSigName(relationship.getEntityNames().get(1));
                Expr e = ArrowExpr.of(QualNameExpr.of(leftName), QualNameExpr.of(rightName));
                for (int i = 2; i < arity; i++) {
                    e = ArrowExpr.of(e,
                                     QualNameExpr.of(genSym.entityToSigName(relationship.getEntityNames()
                                                                                        .get(i))));
                }
                expr = e;
            }
            stateDecls.add(Decl.of(name, expr));
        }
        final SigDecl state = SigDecl.of(genSym.stateSigName(), stateDecls);

        return Collections.singletonList(state);
    }

    /**
     * Create an Alloy fact which prevents making orphan entities.
     *
     * <p>
     * Create a fact of the form
     *
     * <pre>
     * fact { all e: SomeEntity | some s: State | e in (s.someEntity) ... }"
     * </pre>
     *
     * <p>
     * for all entities.
     *
     * @param erminTuple ErminTuple from which the fact is made
     * @return Paragraphs which represents the fact
     */
    private Collection<Paragraph> noOrphanEntitiesFact(ErminTuple erminTuple, GenSym genSym) {
        final List<Expr> noOrphanExprs = new ArrayList<>();
        for (ErminEntity entity : erminTuple.getEntities()) {
            final String entityDeclName = genSym.entityToDeclName(entity.getName());
            final String entitySigName = genSym.entityToSigName(entity.getName());
            final GenSym genLocalVars = genSym.createChild();
            final String entityVar = genLocalVars.genShort(entityDeclName);
            final String stateVar = genLocalVars.genShort("state");

            // s.entity
            final BinOpExpr currEntities =
                BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(entityDeclName));
            // e in s.entity
            final CompareExpr inExpr = CompareExpr.in(QualNameExpr.of(entityVar), currEntities);
            // some s: State | e in s.entity
            final QuantExpr innerQuant =
                QuantExpr.some(stateVar, QualNameExpr.of(genSym.stateSigName()), inExpr);
            // all e: Entity | some s: State | e in s.entity
            final QuantExpr outerQuant = QuantExpr.all(entityVar, QualNameExpr.of(entitySigName), innerQuant);

            noOrphanExprs.add(outerQuant);
        }
        final FactDecl noOrphanEntities =
            FactDecl.of(genSym.gen("noOrphanEntities"), new Block(noOrphanExprs));

        return Collections.singletonList(noOrphanEntities);
    }

    private Decl toAlloyDecl(ErminDecl decl) {
        return Decl.of(decl.getVarName().toLowerCamel(),
                       QualNameExpr.of(decl.getEntityName().toUpperCamel()));
    }

    private Expr toAlloyExpr(ErminBoolExp boolExp) {
        final AlloyTranslator t = this;
        return boolExp.accept(new ErminBoolExpVisitor<Expr>() {

            @Override
            public Expr visitUniversalBoolExp(ErminUniversalBoolExp universalBoolExp) {
                return new QuantExpr(Quant.ALL,
                                     universalBoolExp.getDecls()
                                                     .stream()
                                                     .map(t::toAlloyDecl)
                                                     .collect(Collectors.toList()),
                                     Collections.singletonList(toAlloyExpr(universalBoolExp.getBody())));
            }

            @Override
            public Expr visitEqualBoolExp(ErminEqualBoolExp equalBoolExp) {
                return CompareExpr.eq(toAlloyExpr(equalBoolExp.getLeft()),
                                      toAlloyExpr(equalBoolExp.getRight()));
            }

            @Override
            public Expr visitNotEqualBoolExp(ErminNotEqualBoolExp notEqualBoolExp) {
                return CompareExpr.notEq(toAlloyExpr(notEqualBoolExp.getLeft()),
                                      toAlloyExpr(notEqualBoolExp.getRight()));
            }
        });
    }

    private Expr toAlloyExpr(ErminExp erminExp) {
        final AlloyTranslator t = this;
        return erminExp.accept(new ErminExpVisitor<Expr>() {

            @Override
            public Expr visitVarExp(ErminVarExp varExp) {
                return QualNameExpr.of(varExp.getName());
            }

            @Override
            public Expr visitTupleExp(ErminTupleExp tupleExp) {
                return tupleOf(tupleExp.getExps().stream().map(t::toAlloyExpr).collect(Collectors.toList()));
            }
        });
    }

    /**
     * Generate Alloy predicates which represent abstract processes.
     * @param erminTuple ErminTuple from which predicates are made
     * @param genSym GenSym
     * @return Predicates
     */
    private Collection<Paragraph> abstractProcessPredicates(ErminTuple erminTuple, GenSym genSym) {
        final List<Paragraph> paragraphs = new ArrayList<>();
        for (ErminAbstractProcess abstractProcess : erminTuple.getAbstractProcesses()) {
            final String processName = genSym.processToPredName(abstractProcess.getName());
            final GenSym genLocalVar = genSym.createChild();

            // Construct parameters
            final List<Decl> paraDecls = new ArrayList<>();
            final String now = genLocalVar.genShort("t");
            final String next = genLocalVar.genShort("t");
            paraDecls.add(Decl.of(now, QualNameExpr.of(genSym.stateSigName())));
            paraDecls.add(Decl.of(next, QualNameExpr.of(genSym.stateSigName())));
            final Map<String, String> argumentNames = new HashMap<>();
            for (ErminArgument argument : abstractProcess.getArguments()) {
                final ErminName entityName = argument.getEntityName();
                final String argumentName = genLocalVar.genShort(entityName.toLowerCamel());
                argumentNames.put(argument.getVarName(), argumentName);
                paraDecls.add(Decl.of(argumentName, QualNameExpr.of(entityName.toUpperCamel())));
            }

            final List<Expr> exprs = new ArrayList<>();

            // Construct guard conditions from arguments
            for (ErminArgument argument : abstractProcess.getArguments()) {
                final ErminName entityName = argument.getEntityName();
                // t.entity
                final Expr currEntities =
                    BinOpExpr.dot(QualNameExpr.of(now), QualNameExpr.of(entityName.toLowerCamel()));
                final String argumentName = argumentNames.get(argument.getVarName());
                exprs.add(argument.accept(new ErminArgumentVisitor<Expr>() {

                    @Override
                    public Expr visitNewEntityArgument(ErminNewEntityArgument newEntityArgument) {
                        // e not in t.entity
                        return CompareExpr.notIn(QualNameExpr.of(argumentName), currEntities);
                    }

                    @Override
                    public Expr visitExistingEntityArgument(
                            ErminExistingEntityArgument existingEntityArgument) {
                        // e in t.entity
                        return CompareExpr.in(QualNameExpr.of(argumentName), currEntities);
                    }
                }));
            }
            // Construct guard conditions from guard clause
            for (ErminBoolExp guard : abstractProcess.getGuards()) {
                exprs.add(toAlloyExpr(guard));
            }

            // Construct body
            final Map<ErminName, String> lastUpdated = new HashMap<>();

            final List<LetDecl> letDecls = new ArrayList<>();
            for (ErminEntity entity : erminTuple.getEntities()) {
                // t.entity
                final Expr currEntities =
                    BinOpExpr.dot(QualNameExpr.of(now), QualNameExpr.of(entity.getName().toLowerCamel()));
                final String varName = genLocalVar.gen(entity.getName().toLowerCamel());
                // entity' = t.entity
                letDecls.add(new LetDecl(varName, currEntities));
                lastUpdated.put(entity.getName(), varName);
            }
            for (ErminRelationship relationship : erminTuple.getRelationships()) {
                // t.relationship
                final Expr currRelationships =
                    BinOpExpr.dot(QualNameExpr.of(now),
                                  QualNameExpr.of(relationship.getName().toLowerCamel()));
                final String varName = genLocalVar.gen(relationship.getName().toLowerCamel());
                // relationship' = t.relationship
                letDecls.add(new LetDecl(varName, currRelationships));
                lastUpdated.put(relationship.getName(), varName);
            }
            final List<Expr> body = new ArrayList<>();
            exprs.add(new LetExpr(letDecls, new Block(body)));

            List<Expr> innerMostBlock = body;
            for (ErminStatement statement : abstractProcess.getStatements()) {
                final List<Expr> blockToAppend = innerMostBlock;
                innerMostBlock = statement.accept(new ErminStatementVisitor<List<Expr>>() {

                    private Expr erminExpToAlloyExpr(ErminExp exp) {
                        return exp.accept(new ErminExpVisitor<Expr>() {

                            @Override
                            public Expr visitVarExp(ErminVarExp varExp) {
                                return QualNameExpr.of(argumentNames.get(varExp.getName()));
                            }

                            @Override
                            public Expr visitTupleExp(ErminTupleExp tupleExp) {
                                final List<ErminExp> exps = tupleExp.getExps();
                                if (exps.size() == 1) {
                                    return erminExpToAlloyExpr(exps.get(0));
                                } else {
                                    Expr tuple = ArrowExpr.of(erminExpToAlloyExpr(exps.get(0)),
                                                              erminExpToAlloyExpr(exps.get(1)));
                                    for (int i = 2; i < exps.size(); i++) {
                                        tuple = ArrowExpr.of(tuple, erminExpToAlloyExpr(exps.get(i)));
                                    }
                                    return tuple;
                                }
                            }
                        });

                    }

                    @Override
                    public List<Expr> visitInsertStatement(ErminInsertStatement insertStatement) {
                        final ErminName name = insertStatement.getLvalue();

                        final Expr expr = erminExpToAlloyExpr(insertStatement.getExp());
                        // entity' + expr
                        final Expr insert = BinOpExpr.plus(QualNameExpr.of(lastUpdated.get(name)), expr);

                        final String lvalue = genLocalVar.gen(name.toLowerCamel());
                        lastUpdated.put(name, lvalue);

                        // entity'' = entity' + expr
                        final List<LetDecl> letDecls = Collections.singletonList(new LetDecl(lvalue, insert));

                        final List<Expr> body = new ArrayList<>();
                        // let entity'' = entity' + expr { ... }
                        blockToAppend.add(new LetExpr(letDecls, new Block(body)));

                        return body;
                    }

                    @Override
                    public List<Expr> visitDeleteStatement(ErminDeleteStatement deleteStatement) {
                        final ErminName entityName = deleteStatement.getLvalue();

                        final Expr expr = erminExpToAlloyExpr(deleteStatement.getExp());
                        // entity' - expr
                        final Expr insert =
                            BinOpExpr.minus(QualNameExpr.of(lastUpdated.get(entityName)), expr);

                        final String lvalue = genLocalVar.gen(entityName.toLowerCamel());
                        lastUpdated.put(entityName, lvalue);

                        // entity'' = entity' - expr
                        final List<LetDecl> letDecls = Collections.singletonList(new LetDecl(lvalue, insert));

                        final List<Expr> body = new ArrayList<>();
                        // let entity'' = entity' + expr { ... }
                        blockToAppend.add(new LetExpr(letDecls, new Block(body)));

                        return body;
                    }

                    @Override
                    public List<Expr> visitUpdateStatement(ErminUpdateStatement updateStatement) {
                        final ErminName relationshipName = updateStatement.getLvalue();
                        final ErminRelationship relationship =
                            erminTuple.getRelationships()
                                      .stream()
                                      .filter(r -> r.getName().equals(relationshipName))
                                      .findFirst()
                                      .orElseThrow(() -> new IllegalStateException(relationshipName.toSnake()
                                              + " is not relationship"));
                        if (!(relationship instanceof ErminBinaryRelationship)) {
                            throw new IllegalStateException(relationshipName.toSnake()
                                    + " is not binary relationship");
                        }
                        final ErminBinaryRelationship binaryRelationship =
                            (ErminBinaryRelationship) relationship;
                        switch (binaryRelationship.getLeft().getMultiplicity()) {
                            case ONE:
                            case ZERO_OR_ONE:
                                switch (binaryRelationship.getRight().getMultiplicity()) {
                                    case ONE:
                                    case ZERO_OR_ONE:
                                        throw new IllegalStateException("cannot determine key for update");
                                    case ZERO_OR_MORE:
                                    case ONE_OR_MORE:
                                        throw new UnsupportedOperationException("unimplemented");
                                }
                                break;
                            case ZERO_OR_MORE:
                            case ONE_OR_MORE:
                                switch (binaryRelationship.getRight().getMultiplicity()) {
                                    case ONE:
                                    case ZERO_OR_ONE:
                                        final Expr tuple =
                                            tupleOf(updateStatement.getExps()
                                                                   .stream()
                                                                   .map(this::erminExpToAlloyExpr)
                                                                   .collect(Collectors.toList()));

                                        // relationship' ++ tuple
                                        final Expr insert =
                                            BinOpExpr.override(QualNameExpr.of(lastUpdated.get(relationshipName)),
                                                               tuple);

                                        final String lvalue =
                                            genLocalVar.gen(relationshipName.toLowerCamel());
                                        lastUpdated.put(relationshipName, lvalue);

                                        // relationship'' = relationship' ++ tuple
                                        final List<LetDecl> letDecls =
                                            Collections.singletonList(new LetDecl(lvalue, insert));

                                        final List<Expr> body = new ArrayList<>();
                                        // let relationship'' = relationship' ++ tuple { ... }
                                        blockToAppend.add(new LetExpr(letDecls, new Block(body)));

                                        return body;
                                    case ZERO_OR_MORE:
                                    case ONE_OR_MORE:
                                        throw new IllegalStateException("cannot determine key for update");

                                }
                                break;
                        }
                        throw new IllegalStateException("should never reach here");
                    }
                });
            }

            for (ErminEntity entity : erminTuple.getEntities()) {
                final Expr newEntities =
                    BinOpExpr.dot(QualNameExpr.of(next), QualNameExpr.of(entity.getName().toLowerCamel()));
                final Expr transition =
                    CompareExpr.eq(newEntities, QualNameExpr.of(lastUpdated.get(entity.getName())));
                innerMostBlock.add(transition);
            }
            for (ErminRelationship relationship : erminTuple.getRelationships()) {
                final Expr newRelationship =
                    BinOpExpr.dot(QualNameExpr.of(next),
                                  QualNameExpr.of(relationship.getName().toLowerCamel()));
                final Expr transition =
                    CompareExpr.eq(newRelationship, QualNameExpr.of(lastUpdated.get(relationship.getName())));
                innerMostBlock.add(transition);
            }

            // Wrap up
            final PredDecl predDecl = PredDecl.of(processName, paraDecls, new Block(exprs));
            paragraphs.add(predDecl);
        }
        return paragraphs;
    }

    private Expr tupleOf(List<Expr> exprs) {
        Expr tuple = exprs.get(0);
        for (int i = 1; i < exprs.size(); i++) {
            tuple = BinOpExpr.arrow(tuple, exprs.get(i));
        }
        return tuple;
    }

    private Collection<Paragraph> initPredicate(ErminTuple erminTuple, GenSym genSym) {
        final String predName = genSym.initPredName();
        final GenSym genLocalVars = genSym.createChild();
        final String stateVar = genLocalVars.genShort("state");
        final List<Expr> exprs = new ArrayList<>();
        for (ErminEntity entity : erminTuple.getEntities()) {
            exprs.add(new UnOpExpr(UnOp.NO,
                                   BinOpExpr.dot(QualNameExpr.of(stateVar),
                                                 QualNameExpr.of(genSym.entityToDeclName(entity.getName())))));

        }
        for (ErminRelationship relationship : erminTuple.getRelationships()) {
            exprs.add(new UnOpExpr(UnOp.NO,
                                   BinOpExpr.dot(QualNameExpr.of(stateVar),
                                                 QualNameExpr.of(genSym.relationshipToDeclName(relationship.getName())))));
        }
        final PredDecl init =
            PredDecl.of(predName,
                        Arrays.asList(Decl.of(stateVar, QualNameExpr.of(genSym.stateSigName()))),
                        new Block(exprs));
        return Collections.singletonList(init);
    }

    private Collection<Paragraph> tracesFact(ErminTuple erminTuple, GenSym genSym) {
        final List<Expr> exprs = new ArrayList<>();

        // first.init
        final Expr initPred = BinOpExpr.dot(QualNameExpr.of("first"), QualNameExpr.of(genSym.initPredName()));
        exprs.add(initPred);

        final GenSym genOuterLocalVars = genSym.createChild();
        final String stateVar = genOuterLocalVars.genShort("state");

        Expr alternatives = null;
        for (ErminAbstractProcess abstractProcess : erminTuple.getAbstractProcesses()) {
            final GenSym genInnerLocalVars = genOuterLocalVars.createChild();
            final List<Decl> decls = new ArrayList<>();
            final List<Expr> boxExprs = new ArrayList<>();
            boxExprs.add(QualNameExpr.of(stateVar));
            boxExprs.add(BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of("next")));
            for (ErminArgument argument : abstractProcess.getArguments()) {
                final String argumentVar =
                    genInnerLocalVars.genShort(argument.getEntityName().toLowerCamel());
                decls.add(Decl.of(argumentVar,
                                  QualNameExpr.of(genSym.entityToSigName(argument.getEntityName()))));
                boxExprs.add(QualNameExpr.of(argumentVar));
            }
            final Expr step =
                new QuantExpr(Quant.SOME,
                              decls,
                              Arrays.asList(new BoxExpr(QualNameExpr.of(genSym.processToPredName(abstractProcess.getName())),
                                                        boxExprs)));
            if (alternatives == null) {
                alternatives = step;
            } else {
                alternatives = BinOpExpr.or(alternatives, step);
            }
        }
        if (alternatives == null) {
            return Collections.emptyList();
        }
        final Expr stateMinusLast =
            BinOpExpr.minus(QualNameExpr.of(genSym.stateSigName()), QualNameExpr.of("last"));
        exprs.add(QuantExpr.all(stateVar, stateMinusLast, alternatives));
        return Collections.singletonList(FactDecl.of(new Block(exprs)));
    }

    private Collection<Paragraph> multiplicityAssertion(ErminTuple erminTuple, GenSym genSym, int scope) {
        final List<Paragraph> paragraphs = new ArrayList<>();
        // Create assertions on multiplicity of binary relationships
        for (ErminRelationship relationship : erminTuple.getRelationships()) {
            relationship.accept(new ErminRelationshipVisitor<Void>() {

                @Override
                public Void visitBinaryRelationship(ErminBinaryRelationship binaryRelationship) {
                    final String name = binaryRelationship.getName().toLowerCamel();
                    final ErminRelationshipExp expLeft = binaryRelationship.getLeft();
                    final ErminRelationshipExp expRight = binaryRelationship.getRight();
                    paragraphs.addAll(multiplicityAssertion(expLeft, expRight, name));
                    paragraphs.addAll(multiplicityAssertion(expRight, expLeft, name));
                    return null;
                }

                @Override
                public Void visitMultiRelationship(ErminMultiRelationship multiRelationship) {
                    return null;
                }

                private Collection<Paragraph> multiplicityAssertion(ErminRelationshipExp exp1,
                        ErminRelationshipExp exp2, String name) {
                    if (exp2.getMultiplicity() == ErminMultiplicity.ZERO_OR_MORE) {
                        return Collections.emptyList();
                    }

                    final String exp1Name = exp1.getName().toLowerCamel();
                    final String exp2Name = exp2.getName().toLowerCamel();
                    final GenSym genLocalVars = genSym.createChild();
                    final String stateVar = genLocalVars.genShort("state");
                    final String leftEntityVar = genLocalVars.genShort(exp1Name);
                    final String rightEntityVar = genLocalVars.genShort(exp2Name);
                    final Expr currLeftEntity =
                        BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(exp1Name));
                    final Expr currRightEntity =
                        BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(exp2Name));
                    final Expr currRelationship =
                        BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(name));
                    final CompareExpr eqExpr =
                        CompareExpr.eq(BinOpExpr.dot(QualNameExpr.of(leftEntityVar), currRelationship),
                                       QualNameExpr.of(rightEntityVar));
                    final Quant quant;
                    switch (exp2.getMultiplicity()) {
                        case ZERO_OR_ONE:
                            quant = Quant.LONE;
                            break;
                        case ONE:
                            quant = Quant.ONE;
                            break;
                        case ONE_OR_MORE:
                            quant = Quant.SOME;
                            break;
                        default:
                            throw new IllegalStateException("should never reach here");
                    }
                    final QuantExpr innerQuant = QuantExpr.of(quant, rightEntityVar, currRightEntity, eqExpr);
                    final QuantExpr outerQuant =
                        new QuantExpr(Quant.ALL,
                                      Arrays.asList(Decl.of(stateVar, QualNameExpr.of(genSym.stateSigName())),
                                                    Decl.of(leftEntityVar, currLeftEntity)),
                                      Arrays.asList(innerQuant));
                    // all s: State | all b: s.bug | one a: s.account | b.(s.reportedBy) = a
                    final String assertionName =
                        genSym.gen(name + exp2.getName().toUpperCamel() + "Multiplicity");
                    return Arrays.asList(AssertDecl.of(assertionName, Block.of(outerQuant)),
                                         CmdDecl.check(assertionName, Scope.of(scope)));
                }
            });
        }
        return paragraphs;
    }

    private Expr projection(Expr expr, int columnIndex, int numColumns) {
        Expr projected = expr;
        for (int i = 0; i < columnIndex; i++) {
            projected = BinOpExpr.dot(QualNameExpr.of("univ"), projected);
        }
        for (int i = 0; i < numColumns - columnIndex - 1; i++) {
            projected = BinOpExpr.dot(projected, QualNameExpr.of("univ"));
        }
        return projected;
    }

    /**
     * Create assertion for prohibiting dangling references.
     * @param erminTuple ErminTuple from which the assertions are made
     * @return The assertions
     */
    private Collection<Paragraph> noDanglingReferenceAssertion(ErminTuple erminTuple, GenSym genSym,
            int scope) {
        final List<Paragraph> paragraphs = new ArrayList<>();
        for (ErminRelationship relationship : erminTuple.getRelationships()) {
            final String name = relationship.getName().toLowerCamel();
            final int arity = relationship.getEntityNames().size();
            int i = 0;
            for (ErminName expName : relationship.getEntityNames()) {
                final String assertionName =
                    genSym.gen(relationship.getName().toLowerCamel() + expName.toUpperCamel() + "MustExist");
                final GenSym genLocalVar = genSym.createChild();
                final String stateVar = genLocalVar.genShort("state");
                final Expr currRelationship = BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(name));
                final Expr projected = projection(currRelationship, i, arity);
                // s.entity
                final Expr currEntities =
                    BinOpExpr.dot(QualNameExpr.of(stateVar), QualNameExpr.of(expName.toLowerCamel()));
                // ... in s.entity
                final Expr inExp = CompareExpr.in(projected, currEntities);
                // all s: State | ... in s.entity
                final QuantExpr quant = QuantExpr.all(stateVar, QualNameExpr.of("State"), inExp);
                paragraphs.add(AssertDecl.of(assertionName, Block.of(quant)));
                paragraphs.add(CmdDecl.check(assertionName, Scope.of(scope)));
                i++;
            }
        }
        return paragraphs;
    }
}
