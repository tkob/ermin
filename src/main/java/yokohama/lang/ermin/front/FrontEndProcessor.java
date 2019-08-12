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
import yokohama.lang.ermin.Absyn.EntityDef;
import yokohama.lang.ermin.Absyn.IdentifierDef;
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.ListAttribute;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminKey;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;

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

        return new ErminTuple(codeResolver, typeResolver, entityResolver, entities);
    }

    public ErminEntity toErminEntity(EntityDef entityDef, TypeResolver typeResolver,
            TypeResolver identifierResolver, Collection<ErminName> entityNames) {
        ErminName entityName = ErminName.fromSnake(entityDef.ident_);

        final List<ErminKey> identifierKeys = new ArrayList<>();
        final List<ErminName> entityKeys = new ArrayList<>();
        for (String keyRef : entityDef.listident_) {
            ErminName name = ErminName.fromSnake(keyRef);
            identifierResolver.ifResolvedOrElse(keyRef, (type -> {
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
}
