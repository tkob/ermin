package yokohama.lang.ermin.back.reladomo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminKey;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.CodeResolver;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.ermin.front.Resolver;
import yokohama.lang.ermin.relationship.ErminMultiplicity;
import yokohama.lang.ermin.relationship.ErminRelationship;
import yokohama.lang.ermin.relationship.ErminRelationshipExp;
import yokohama.lang.reladomo.AttributePureType;
import yokohama.lang.reladomo.AttributeType;
import yokohama.lang.reladomo.CardinalityType;
import yokohama.lang.reladomo.MithraObjectType;
import yokohama.lang.reladomo.ObjectFactory;
import yokohama.lang.reladomo.ObjectType;
import yokohama.lang.reladomo.RelationshipType;

public class ReladomoTranslator {

    FrontEndProcessor frontEndProcessor = new FrontEndProcessor();

    ObjectFactory factory = new ObjectFactory();

    public Iterable<MithraObjectType> toMithraObjects(ErminTuple erminTuple) {
        final Map<ErminName, MithraObjectType> mithraObjects = new HashMap<>();

        // Entities to MithraObjects.
        erminTuple.getEntities().forEach(entity -> {
            mithraObjects.put(entity.getName(),
                              entityToMithraObject(entity,
                                                   erminTuple.getEntityResolver(),
                                                   erminTuple.getCodeResolver()));
        });

        // Relations to MithraObjects. Possibly modifies existing entities.
        erminTuple.getRelationships().forEach(relationship -> {
            relationshipToMithraObject(relationship,
                                       erminTuple.getEntityResolver(),
                                       erminTuple.getCodeResolver(),
                                       mithraObjects);
        });

        // Codes to MithraObjects
        erminTuple.getCodeResolver().getNames().forEach(name -> {
            mithraObjects.put(name, codeToMithraObject(name, erminTuple.getCodeResolver()));
        });

        return mithraObjects.values();
    }

    void accumulatePrimaryKeys(ErminEntity entity, Resolver<ErminName, ErminEntity> entityResolver,
            CodeResolver codeResolver, List<AttributeType> attributes) {

        entity.getEntityKeys().forEach(entityKey -> {
            entityResolver.resolve(entityKey).ifPresent(keyEntity -> {
                accumulatePrimaryKeys(keyEntity, entityResolver, codeResolver, attributes);
            });
        });
        final ErminKey typeKey = entity.getTypeKey();
        final AttributeType attributeType = factory.createAttributeType();
        attributeType.setName(typeKey.getName().toLowerCamel());
        attributeType.setColumnName(typeKey.getName().toSnake());
        attributeType.setPrimaryKey(true);
        typeKey.getType().accept(new ReladomoJavaTypeSetter(attributeType, codeResolver));
        attributes.add(attributeType);

    }

    MithraObjectType entityToMithraObject(ErminEntity entity, Resolver<ErminName, ErminEntity> entityResolver,
            CodeResolver codeResolver) {
        final MithraObjectType mithraObject = factory.createMithraObjectType();

        mithraObject.setObjectType(ObjectType.TRANSACTIONAL);
        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(entity.getName().toUpperCamel());
        mithraObject.setDefaultTable(entity.getName().toSnake());
        final List<AttributeType> attributes = mithraObject.getAttribute();

        accumulatePrimaryKeys(entity, entityResolver, codeResolver, attributes);

        // add non-key attributes
        attributes.addAll(entity.getAttributes()
                                .stream()
                                .map(attribute -> toAttribute(attribute, codeResolver))
                                .collect(Collectors.toList()));

        return mithraObject;
    }

    AttributeType toAttribute(ErminAttribute attribute, CodeResolver codeResolver) {
        final AttributeType attributeType = factory.createAttributeType();

        attributeType.setName(attribute.getName().toLowerCamel());
        attributeType.setColumnName(attribute.getName().toSnake());
        switch (attribute.getAttributeSpecifier()) {
            case MANDATORY:
            case UNIQUE:
                attributeType.setNullable(false);
                break;
            case OPTIONAL:
                attributeType.setNullable(true);
                break;
        }
        attributeType.setPrimaryKey(false);
        attribute.getType().accept(new ReladomoJavaTypeSetter(attributeType, codeResolver));

        return attributeType;
    }

    void relationshipToMithraObject(ErminRelationship relationship,
            Resolver<ErminName, ErminEntity> entityResolver, CodeResolver codeResolver,
            Map<ErminName, MithraObjectType> mithraObjects) {
        final Iterable<ErminName> exps = relationship.getEntityNames();

        final MithraObjectType mithraObject = factory.createMithraObjectType();
        mithraObject.setObjectType(ObjectType.TRANSACTIONAL);
        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(relationship.getName().toUpperCamel());
        mithraObject.setDefaultTable(relationship.getName().toSnake());

        final List<AttributeType> attributes = mithraObject.getAttribute();
        final List<RelationshipType> relationshipTypes = mithraObject.getRelationship();
        for (ErminName name : exps) {
            final List<AttributeType> primaryKeys = mithraObjects.get(name)
                                                                 .getAttribute()
                                                                 .stream()
                                                                 .filter(AttributePureType::isPrimaryKey)
                                                                 .collect(Collectors.toList());
            attributes.addAll(primaryKeys);

            final RelationshipType relationshipType = factory.createRelationshipType();
            relationshipType.setCardinality(CardinalityType.MANY_TO_ONE);
            relationshipType.setName(name.toLowerCamel());
            relationshipType.setRelatedObject(name.toUpperCamel());
            relationshipType.setValue(primaryKeys.stream()
                                                 .map(attr -> "this." + attr.getName() + " = "
                                                         + name.toUpperCamel() + "." + attr.getName())
                                                 .collect(Collectors.joining(" and ")));
            relationshipTypes.add(relationshipType);
        }

        mithraObjects.put(relationship.getName(), mithraObject);

        // Add Relationship elements to existing entities if the arity is 2.
        relationship.applyBiFunction((ErminRelationshipExp left, ErminRelationshipExp right) -> {
            final List<RelationshipType> relationships = mithraObjects.get(left.getName()).getRelationship();
            final RelationshipType relationshipType = factory.createRelationshipType();
            relationshipType.setName(relationship.getName().toLowerCamel());
            relationshipType.setRelatedObject(right.getName().toUpperCamel());
            relationshipType.setCardinality(CardinalityType.fromValue(translateMultiplicity(left.getMultiplicity())
                    + "-to-" + translateMultiplicity(right.getMultiplicity())));

            final List<AttributeType> leftPrimaryKeys = new ArrayList<>();
            final List<AttributeType> rightPrimaryKeys = new ArrayList<>();
            accumulatePrimaryKeys(entityResolver.resolveOrThrow(left.getName()),
                                  entityResolver,
                                  codeResolver,
                                  leftPrimaryKeys);
            accumulatePrimaryKeys(entityResolver.resolveOrThrow(right.getName()),
                                  entityResolver,
                                  codeResolver,
                                  rightPrimaryKeys);
            final Stream<String> ls = leftPrimaryKeys.stream()
                                                     .map(attribute -> "this." + attribute.getName() + " = "
                                                             + relationship.getName().toUpperCamel() + "."
                                                             + attribute.getName());
            final Stream<String> rs =
                rightPrimaryKeys.stream()
                                .map(attribute -> right.getName().toUpperCamel() + "." + attribute.getName()
                                        + " = " + relationship.getName().toUpperCamel() + "."
                                        + attribute.getName());
            relationshipType.setValue(Stream.concat(ls, rs).collect(Collectors.joining(" and ")));

            relationships.add(relationshipType);
            return null;
        });

    }

    String translateMultiplicity(ErminMultiplicity multiplicity) {
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

        mithraObject.setObjectType(ObjectType.READ_ONLY);
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
