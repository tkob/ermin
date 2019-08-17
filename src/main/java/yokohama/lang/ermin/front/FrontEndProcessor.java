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
import yokohama.lang.ermin.Absyn.CodeDef;
import yokohama.lang.ermin.Absyn.Def;
import yokohama.lang.ermin.Absyn.DefaultEntityRelationship;
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.EntityRelationship;
import yokohama.lang.ermin.Absyn.IdentifierDef;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.ListAttribute;
import yokohama.lang.ermin.Absyn.Multiplicity;
import yokohama.lang.ermin.Absyn.NumericOneMultiplicity;
import yokohama.lang.ermin.Absyn.NumericOneOreMoreMultiplicity;
import yokohama.lang.ermin.Absyn.NumericZeroOrMoreMultiplicity;
import yokohama.lang.ermin.Absyn.NumericZeroOrOneMultiplicity;
import yokohama.lang.ermin.Absyn.OneMultiplicity;
import yokohama.lang.ermin.Absyn.OneOreMoreMultiplicity;
import yokohama.lang.ermin.Absyn.ProductRelationship;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.RelationshipType;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.Absyn.ZeroOrMoreMultiplicity;
import yokohama.lang.ermin.Absyn.ZeroOrOneMultiplicity;
import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminKey;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.relationship.ErminAtomicRelationshipExp;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminProductRelationshipExp;
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
        final TypeResolver typeResolver = typeResolverFactory.fromAbsyn(top,
                codeResolver);
        final TypeResolver identifierResolver = identifierResolverFactory.fromAbsyn(top);

        final Map<ErminName, ErminEntity> nameToEntity = top.accept(
                new Top.Visitor<Map<ErminName, ErminEntity>, TypeResolver>() {
                    @Override
                    public Map<ErminName, ErminEntity> visit(final TopDefinitions p,
                            final TypeResolver typeResolver) {
                        List<EntityDef> entityDefs = filterEntityDef(p.listdef_.stream())
                                .collect(Collectors.toList());

                        List<ErminName> entityNames = entityDefs.stream().map(
                                entityDef -> ErminName.fromSnake(entityDef.ident_))
                                .collect(Collectors.toList());

                        Map<ErminName, ErminEntity> nameToEntity = new HashMap<>();
                        entityDefs.forEach(entityDef -> {
                            ErminEntity entity = toErminEntity(entityDef, typeResolver,
                                    identifierResolver, entityNames);
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

        final Collection<ErminRelationship> relationships = top.accept(
                new Top.Visitor<Collection<ErminRelationship>, TypeResolver>() {

                    @Override
                    public Collection<ErminRelationship> visit(final TopDefinitions p,
                            final TypeResolver arg) {
                        return filterRelationshipDef(p.listdef_.stream()).map(
                                relationshipDef -> toErminRelationship(relationshipDef))
                                .collect(Collectors.toList());
                    }
                }, typeResolver);

        return new ErminTuple(codeResolver, typeResolver, entityResolver, entities, relationships);
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

        List<ErminAttribute> attributes = entityDef.listattribute_.stream().map(
                attribute -> attribute.accept(absynAttributeToErminAttribute,
                        typeResolver)).collect(Collectors.toList());

        return new ErminEntity(entityName, identifierKey, entityKeys, attributes);
    }

    public ErminRelationship toErminRelationship(final RelationshipDef relationshipDef) {
        ErminName name = ErminName.fromSnake(relationshipDef.ident_);
        ErminRelationshipExp exp = toErminRelationshipExp(
                relationshipDef.relationshiptype_);
        return new ErminRelationship(name, exp);
    }

    public ErminRelationshipExp toErminRelationshipExp(
            final RelationshipType relationshiptype_) {
        return relationshiptype_.accept(
                new RelationshipType.Visitor<ErminRelationshipExp, Void>() {

                    @Override
                    public ErminRelationshipExp visit(ProductRelationship p, Void arg) {
                        return new ErminProductRelationshipExp(toErminRelationshipExp(
                                p.relationshiptype_1), toErminRelationshipExp(
                                        p.relationshiptype_2));
                    }

                    @Override
                    public ErminRelationshipExp visit(EntityRelationship p, Void arg) {
                        return new ErminAtomicRelationshipExp(p.multiplicity_.accept(
                                new Multiplicity.Visitor<ErminMultiplicity, Void>() {

                                    @Override
                                    public ErminMultiplicity visit(OneMultiplicity p,
                                            Void arg) {
                                        return ErminMultiplicity.ONE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            ZeroOrOneMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ZERO_OR_ONE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            ZeroOrMoreMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ZERO_OR_MORE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            OneOreMoreMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ONE_OR_MORE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            NumericOneMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ONE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            NumericZeroOrOneMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ZERO_OR_ONE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            NumericZeroOrMoreMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ZERO_OR_MORE;
                                    }

                                    @Override
                                    public ErminMultiplicity visit(
                                            NumericOneOreMoreMultiplicity p, Void arg) {
                                        return ErminMultiplicity.ONE_OR_MORE;
                                    }
                                }, null), ErminName.fromSnake(p.ident_));
                    }

                    @Override
                    public ErminRelationshipExp visit(DefaultEntityRelationship p,
                            Void arg) {
                        return new ErminAtomicRelationshipExp(ErminMultiplicity.ZERO_OR_MORE, ErminName
                                .fromSnake(p.ident_));
                    }
                }, null);
    }

    public Stream<EntityDef> filterEntityDef(final Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(
                new Def.Visitor<Stream<EntityDef>, Void>() {

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
                        return Stream.of(
                                new EntityDef(p.ident_, p.listident_, new ListAttribute()));
                    }

                    @Override
                    public Stream<EntityDef> visit(RelationshipDef p, Void arg) {
                        return Stream.<EntityDef> empty();
                    }
                }, null));
    }

    public Stream<RelationshipDef> filterRelationshipDef(final Stream<Def> defs) {
        return defs.flatMap((Def def) -> def.accept(
                new Def.Visitor<Stream<RelationshipDef>, Void>() {

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
                }, null));
    }
}
