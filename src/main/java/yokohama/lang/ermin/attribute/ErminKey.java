package yokohama.lang.ermin.attribute;

import lombok.Value;
import yokohama.lang.ermin.type.ErminType;

@Value
public class ErminKey {

    private ErminName name;

    private ErminType type;
}
