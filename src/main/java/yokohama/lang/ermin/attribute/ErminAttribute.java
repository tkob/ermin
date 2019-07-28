package yokohama.lang.ermin.attribute;

import lombok.Value;
import yokohama.lang.ermin.type.ErminType;

@Value
public class ErminAttribute {
    private AttributeSpecifier attributeSpecifier;

    private ErminName name;

    private ErminType type;
}
