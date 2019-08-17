package yokohama.lang.ermin.back.reladomo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminName;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.CodeResolver;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.ermin.front.Resolver;
import yokohama.lang.reladomo.AttributeType;
import yokohama.lang.reladomo.MithraObjectType;
import yokohama.lang.reladomo.ObjectFactory;

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
