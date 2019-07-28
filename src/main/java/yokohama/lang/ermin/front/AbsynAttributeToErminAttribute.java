package yokohama.lang.ermin.front;

import yokohama.lang.ermin.Absyn.Attribute;
import yokohama.lang.ermin.Absyn.MandatoryAttribute;
import yokohama.lang.ermin.Absyn.OptionalAttribute;
import yokohama.lang.ermin.Absyn.UniqueAttribute;
import yokohama.lang.ermin.attribute.AttributeSpecifier;
import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminName;

public class AbsynAttributeToErminAttribute implements
                                            Attribute.Visitor<ErminAttribute, TypeResolver> {
    AbsynTypeToErminType absynTypeToErminType = new AbsynTypeToErminType();

    @Override
    public ErminAttribute visit(MandatoryAttribute p, TypeResolver typeResolver) {
        return new ErminAttribute(AttributeSpecifier.MANDATORY, new ErminName(p.ident_), p.type_
                .accept(absynTypeToErminType, typeResolver));
    }

    @Override
    public ErminAttribute visit(OptionalAttribute p, TypeResolver typeResolver) {
        return new ErminAttribute(AttributeSpecifier.OPTIONAL, new ErminName(p.ident_), p.type_
                .accept(absynTypeToErminType, typeResolver));
    }

    @Override
    public ErminAttribute visit(UniqueAttribute p, TypeResolver typeResolver) {
        return new ErminAttribute(AttributeSpecifier.UNIQUE, new ErminName(p.ident_), p.type_
                .accept(absynTypeToErminType, typeResolver));
    }

}
