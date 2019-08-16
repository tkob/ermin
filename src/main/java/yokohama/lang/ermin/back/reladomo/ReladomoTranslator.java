package yokohama.lang.ermin.back.reladomo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.CodeResolver;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.ermin.front.Resolver;
import yokohama.lang.ermin.type.ErminBlobType;
import yokohama.lang.ermin.type.ErminCharType;
import yokohama.lang.ermin.type.ErminClobType;
import yokohama.lang.ermin.type.ErminDateType;
import yokohama.lang.ermin.type.ErminDecimalType;
import yokohama.lang.ermin.type.ErminIntegerType;
import yokohama.lang.ermin.type.ErminStringCodeType;
import yokohama.lang.ermin.type.ErminTypeVisitor;
import yokohama.lang.ermin.type.ErminVarCharType;
import yokohama.lang.reladomo.AttributeType;
import yokohama.lang.reladomo.CardinalityType;
import yokohama.lang.reladomo.MithraObjectType;
import yokohama.lang.reladomo.ObjectFactory;
import yokohama.lang.reladomo.RelationshipType;

public class ReladomoTranslator {

    FrontEndProcessor frontEndProcessor = new FrontEndProcessor();

    ObjectFactory factory = new ObjectFactory();

    public Iterable<MithraObjectType> toMithraObjects(final ErminTuple erminTuple) {
        Stream<MithraObjectType> entities = erminTuple.getEntities().stream().map(
                entityDef -> entityToMithraObject(entityDef, erminTuple
                        .getEntityResolver(), erminTuple.getEntities(), erminTuple
                                .getCodeResolver()));

        CodeResolver codeResolver = erminTuple.getCodeResolver();
        Stream<MithraObjectType> codes = StreamSupport.stream(codeResolver.getNames()
                .spliterator(), false).map(name -> codeToMithraObject(name,
                        codeResolver));

        return Stream.concat(entities, codes).collect(Collectors.toList());
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
            final Iterable<ErminEntity> entities, final CodeResolver codeResolver) {
        final MithraObjectType mithraObject = factory.createMithraObjectType();

        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(entity.getName().toUpperCamel());
        mithraObject.setDefaultTable(entity.getName().toSnake());
        final List<AttributeType> attributes = mithraObject.getAttribute();
        final List<RelationshipType> relationships = mithraObject.getRelationship();

        accumulatePrimaryKeys(entity, entityResolver, codeResolver, attributes);

        // add non-key attributes
        attributes.addAll(entity.getAttributes().stream().map(attribute -> toAttribute(
                attribute, codeResolver)).collect(Collectors.toList()));

        // add code relationships
        entity.getAttributes().forEach(attribute -> {
            attribute.getType().accept(new ErminTypeVisitor<Void>() {

                @Override
                public Void visitCharType(ErminCharType charType) {
                    return null;
                }

                @Override
                public Void visitVarCharType(ErminVarCharType varCharType) {
                    return null;
                }

                @Override
                public Void visitClobType(ErminClobType clobType) {
                    return null;
                }

                @Override
                public Void visitBlobType(ErminBlobType blobType) {
                    return null;
                }

                @Override
                public Void visitDecimalType(ErminDecimalType decimalType) {
                    return null;
                }

                @Override
                public Void visitIntegerType(ErminIntegerType integerType) {
                    return null;
                }

                @Override
                public Void visitDateType(ErminDateType dateType) {
                    return null;
                }

                @Override
                public Void visitStringCodeType(ErminStringCodeType stringCodeType) {
                    final RelationshipType relationshipType = factory
                            .createRelationshipType();
                    relationshipType.setName(attribute.getName().toLowerCamel());
                    relationshipType.setRelatedObject(stringCodeType.getName().toUpperCamel());
                    relationshipType.setCardinality(CardinalityType.MANY_TO_ONE);
                    relationshipType.setValue("this." + attribute.getName().toLowerCamel()
                            + " = " + stringCodeType.getName().toUpperCamel() + "."
                            + stringCodeType.getName().toLowerCamel());
                    relationships.add(relationshipType);
                    return null;
                }
            });

        });

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
        attribute.getType().accept(
                new ReladomoJavaTypeSetter(attributeType, codeResolver));

        return attributeType;
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
