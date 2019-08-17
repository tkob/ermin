package yokohama.lang.ermin.back.reladomo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.CodeResolver;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.ermin.front.Resolver;
import yokohama.lang.ermin.relationship.ErminAtomicRelationshipExp;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminProductRelationshipExp;
import yokohama.lang.ermin.relationship.ErminRelationship;
import yokohama.lang.ermin.relationship.ErminRelationshipExp;
import yokohama.lang.ermin.relationship.ErminRelationshipExpVisitor;
import yokohama.lang.reladomo.AttributePureType;
import yokohama.lang.reladomo.AttributeType;
import yokohama.lang.reladomo.CardinalityType;
import yokohama.lang.reladomo.MithraObjectType;
import yokohama.lang.reladomo.ObjectFactory;
import yokohama.lang.reladomo.RelationshipType;

public class ReladomoTranslator {

    FrontEndProcessor frontEndProcessor = new FrontEndProcessor();

    ObjectFactory factory = new ObjectFactory();

    public Iterable<MithraObjectType> toMithraObjects(final ErminTuple erminTuple) {
        final Map<ErminName, MithraObjectType> mithraObjects = new HashMap<>();

        // Entities to MithraObjects.
        erminTuple.getEntities().forEach(entity -> {
            mithraObjects.put(entity.getName(), entityToMithraObject(entity, erminTuple
                    .getEntityResolver(), erminTuple.getCodeResolver()));
        });

        // Relations to MithraObjects. Possibly modifies existing entities.
        erminTuple.getRelationships().forEach(relationship -> {
            relationshipToMithraObject(relationship, erminTuple.getEntityResolver(),
                    erminTuple.getCodeResolver(), mithraObjects);
        });

        // Codes to MithraObjects
        erminTuple.getCodeResolver().getNames().forEach(name -> {
            mithraObjects.put(name, codeToMithraObject(name, erminTuple
                    .getCodeResolver()));
        });

        return mithraObjects.values();
    }

    void accumulatePrimaryKeys(final ErminEntity entity,
            final Resolver<ErminName, ErminEntity> entityResolver,
            final CodeResolver codeResolver, final List<AttributeType> attributes) {

        entity.getEntityKeys().forEach(entityKey -> {
            entityResolver.resolve(entityKey).ifPresent(keyEntity -> {
                accumulatePrimaryKeys(keyEntity, entityResolver, codeResolver,
                        attributes);
            });
        });
        entity.getTypeKey().ifPresent(typeKey -> {
            final AttributeType attributeType = factory.createAttributeType();
            attributeType.setName(typeKey.getName().toLowerCamel());
            attributeType.setColumnName(typeKey.getName().toSnake());
            attributeType.setPrimaryKey(true);
            typeKey.getType().accept(
                    new ReladomoJavaTypeSetter(attributeType, codeResolver));
            attributes.add(attributeType);
        });
    }

    MithraObjectType entityToMithraObject(final ErminEntity entity,
            final Resolver<ErminName, ErminEntity> entityResolver,
            final CodeResolver codeResolver) {
        final MithraObjectType mithraObject = factory.createMithraObjectType();

        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(entity.getName().toUpperCamel());
        mithraObject.setDefaultTable(entity.getName().toSnake());
        final List<AttributeType> attributes = mithraObject.getAttribute();

        accumulatePrimaryKeys(entity, entityResolver, codeResolver, attributes);

        // add non-key attributes
        attributes.addAll(entity.getAttributes().stream().map(attribute -> toAttribute(
                attribute, codeResolver)).collect(Collectors.toList()));

        return mithraObject;
    }

    AttributeType toAttribute(ErminAttribute attribute, final CodeResolver codeResolver) {
        final AttributeType attributeType = factory.createAttributeType();

        attributeType.setName(attribute.getName().toLowerCamel());
        attributeType.setColumnName(attribute.getName().toSnake());
        switch (attribute.getAttributeSpecifier()) {
            case MANDATORY:
            case UNIQUE:
                attributeType.setNullable(false);
            case OPTIONAL:
                attributeType.setNullable(true);
        }
        attributeType.setPrimaryKey(false);
        attribute.getType().accept(
                new ReladomoJavaTypeSetter(attributeType, codeResolver));

        return attributeType;
    }

    void relationshipToMithraObject(ErminRelationship relationship,
            final Resolver<ErminName, ErminEntity> entityResolver,
            final CodeResolver codeResolver,
            Map<ErminName, MithraObjectType> mithraObjects) {
        final Iterable<ErminName> exps = flattenRelationshipExps(relationship.getExp());

        final MithraObjectType mithraObject = factory.createMithraObjectType();
        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(relationship.getName().toUpperCamel());
        mithraObject.setDefaultTable(relationship.getName().toSnake());

        final List<AttributeType> attributes = mithraObject.getAttribute();
        for (final ErminName name : exps) {
            List<AttributeType> primaryKeys = mithraObjects.get(name).getAttribute()
                    .stream().filter(AttributePureType::isPrimaryKey).collect(Collectors
                            .toList());
            attributes.addAll(primaryKeys);
        }

        mithraObjects.put(relationship.getName(), mithraObject);

        // Add Relationship elements to existing entities if the arity is 2.
        relationship.applyBiFunction((ErminAtomicRelationshipExp left,
                ErminAtomicRelationshipExp right) -> {
            final List<RelationshipType> relationships = mithraObjects.get(left.getName())
                    .getRelationship();
            RelationshipType relationshipType = factory.createRelationshipType();
            relationshipType.setName(relationship.getName().toLowerCamel());
            relationshipType.setRelatedObject(right.getName().toUpperCamel());
            relationshipType.setCardinality(CardinalityType.fromValue(
                    translateMultiplicity(left.getMultiplicity()) + "-to-"
                            + translateMultiplicity(right.getMultiplicity())));

            List<AttributeType> leftPrimaryKeys = new ArrayList<>();
            List<AttributeType> rightPrimaryKeys = new ArrayList<>();
            accumulatePrimaryKeys(entityResolver.resolveOrThrow(left.getName()),
                    entityResolver, codeResolver, leftPrimaryKeys);
            accumulatePrimaryKeys(entityResolver.resolveOrThrow(right.getName()),
                    entityResolver, codeResolver, rightPrimaryKeys);
            Stream<String> ls = leftPrimaryKeys.stream().map(attribute -> "this."
                    + attribute.getName() + " = " + relationship.getName().toUpperCamel()
                    + "." + attribute.getName());
            Stream<String> rs = rightPrimaryKeys.stream().map(attribute -> right.getName()
                    .toUpperCamel() + "." + attribute.getName() + " = " + relationship
                            .getName().toUpperCamel() + "." + attribute.getName());
            relationshipType.setValue(Stream.concat(ls, rs).collect(Collectors.joining(
                    " and ")));

            relationships.add(relationshipType);
            return null;
        });

    }

    Collection<ErminName> flattenRelationshipExps(final ErminRelationshipExp exp) {
        return exp.accept(new ErminRelationshipExpVisitor<List<ErminName>>() {

            @Override
            public List<ErminName> visitAtomicRelationshipExp(
                    final ErminAtomicRelationshipExp atomicRelationshipExp) {
                return Collections.singletonList(atomicRelationshipExp.getName());
            }

            @Override
            public List<ErminName> visitProductRelationshipExp(
                    final ErminProductRelationshipExp productRelationshipExp) {
                final List<ErminName> union = new ArrayList<>();
                union.addAll(flattenRelationshipExps(productRelationshipExp.getLeft()));
                union.addAll(flattenRelationshipExps(productRelationshipExp.getRight()));
                return union;
            }
        });
    }

    String translateMultiplicity(final ErminMultiplicity multiplicity) {
        switch (multiplicity) {
            case ONE:
            case ZERO_OR_ONE:
                return "one";
            case ZERO_OR_MORE:
            case ONE_OR_MORE:
                return "many";
            default:
                throw new RuntimeException("should never reach here");
        }
    }

    MithraObjectType codeToMithraObject(ErminName name, CodeResolver codeResolver) {
        final MithraObjectType mithraObject = factory.createMithraObjectType();

        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(name.toUpperCamel());
        mithraObject.setDefaultTable(name.toSnake());

        final List<AttributeType> attributes = mithraObject.getAttribute();

        final AttributeType attributeType = factory.createAttributeType();
        attributeType.setName(name.toLowerCamel());
        attributeType.setColumnName(name.toSnake());
        attributeType.setJavaType("String");
        attributeType.setPrimaryKey(true);
        attributeType.setMaxLength(codeResolver.maxLength(name));

        attributes.add(attributeType);

        return mithraObject;
    }
}
