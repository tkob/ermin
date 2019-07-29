package yokohama.lang.ermin.front;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
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
import yokohama.lang.ermin.Absyn.KeyOnlyEntityDef;
import yokohama.lang.ermin.Absyn.ListAttribute;
import yokohama.lang.ermin.Absyn.RelationshipDef;
import yokohama.lang.ermin.Absyn.Top;
import yokohama.lang.ermin.Absyn.TopDefinitions;
import yokohama.lang.ermin.Absyn.TypeDef;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;

public class FrontEndProcessor {

    AbsynAttributeToErminAttribute absynAttributeToErminAttribute = new AbsynAttributeToErminAttribute();

    AbsynTypeToErminType absynTypeToErminType = new AbsynTypeToErminType();

    CodeResolverFactory codeResolverFactory = new CodeResolverFactory();

    TypeResolverFactory typeResolverFactory = new TypeResolverFactory();

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

        final List<ErminEntity> entities = top.accept(
                new Top.Visitor<List<ErminEntity>, TypeResolver>() {
                    @Override
                    public List<ErminEntity> visit(final TopDefinitions p,
                            final TypeResolver typeResolver) {
                        List<EntityDef> entityDefs = filterEntityDef(p.listdef_.stream())
                                .collect(Collectors.toList());
                        List<ErminName> entityNames = entityDefs.stream().map(
                                entityDef -> new ErminName(entityDef.ident_)).collect(
                                        Collectors.toList());
                        return entityDefs.stream().map(entityDef -> toErminEntity(
                                entityDef, typeResolver, entityNames)).collect(Collectors
                                        .toList());
                    }
                }, typeResolver);
        return new ErminTuple(codeResolver, typeResolver, entities);
    }

    public ErminEntity toErminEntity(EntityDef entityDef, TypeResolver typeResolver,
            Collection<ErminName> entityNames) {
        Map<Boolean, List<ErminName>> entityKeysAndOthers = entityDef.listident_.stream()
                .map(ident -> ErminName.fromSnake(ident)).collect(Collectors
                        .partitioningBy(entityName -> entityNames.contains(entityName)));

        List<ErminName> entityKeys = entityKeysAndOthers.get(true);

        Map<Boolean, List<ErminName>> typeKeyAndOthers = entityKeysAndOthers.get(false)
                .stream().collect(Collectors.partitioningBy(entityName -> typeResolver
                        .hasName(entityName.toString())));

        List<ErminName> others = typeKeyAndOthers.get(false);
        if (!others.isEmpty()) {
            throw new RuntimeException(others.stream().map(ErminName::toString).collect(
                    Collectors.joining(" ")));
        }

        List<ErminName> typeKeys = typeKeyAndOthers.get(true);
        Optional<ErminName> typeKey;
        if (typeKeys.isEmpty()) {
            typeKey = Optional.empty();
        } else if (typeKeys.size() == 1) {
            typeKey = Optional.of(typeKeys.get(0));
        } else {
            throw new RuntimeException();
        }

        return new ErminEntity(new ErminName(entityDef.ident_), typeKey, entityKeys, entityDef.listattribute_
                .stream().map(attribute -> attribute.accept(
                        absynAttributeToErminAttribute, typeResolver)).collect(Collectors
                                .toList()));
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
