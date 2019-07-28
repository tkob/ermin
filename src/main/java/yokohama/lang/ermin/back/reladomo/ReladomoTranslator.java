package yokohama.lang.ermin.back.reladomo;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.entity.ErminEntity;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.ermin.front.TypeResolver;
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
        return erminTuple.getEntities().stream().map(entityDef -> toMithraObject(
                entityDef, erminTuple.getTypeResolver())).collect(Collectors.toList());
    }

    MithraObjectType toMithraObject(final ErminEntity entity,
            final TypeResolver typeResolver) {
        final MithraObjectType mithraObject = factory.createMithraObjectType();

        mithraObject.setPackageName("yokohama.lang.test");
        mithraObject.setClassName(entity.getName().toString());
        mithraObject.setDefaultTable(entity.getName().toString());
        final List<AttributeType> attributes = mithraObject.getAttribute();
        final List<RelationshipType> relationships = mithraObject.getRelationship();

        // add non-key attributes
        attributes.addAll(entity.getAttributes().stream().map(attribute -> toAttribute(
                attribute, typeResolver)).collect(Collectors.toList()));

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
                    relationshipType.setName(attribute.getName().toString());
                    relationshipType.setRelatedObject(stringCodeType.getName());
                    relationshipType.setCardinality(CardinalityType.MANY_TO_ONE);
                    relationshipType.setValue("this." + attribute.getName().toString()
                            + " = " + stringCodeType.getName() + ".code");
                    relationships.add(relationshipType);
                    return null;
                }
            });

        });

        return mithraObject;
    }

    AttributeType toAttribute(ErminAttribute attribute, final TypeResolver typeResolver) {
        final AttributeType attributeType = factory.createAttributeType();

        attributeType.setName(attribute.getName().toString());
        attributeType.setColumnName(attribute.getName().toString());
        switch (attribute.getAttributeSpecifier()) {
            case MANDATORY:
            case UNIQUE:
                attributeType.setNullable(false);
            case OPTIONAL:
                attributeType.setNullable(true);
        }
        attribute.getType().accept(new ReladomoJavaTypeSetter(attributeType));

        return attributeType;
    }

}
