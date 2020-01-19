package yokohama.lang.ermin.process;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminDecl {
    private final ErminName varName;

    private final ErminName entityName;
}
