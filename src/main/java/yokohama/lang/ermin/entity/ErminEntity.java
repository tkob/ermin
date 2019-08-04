package yokohama.lang.ermin.entity;

import java.util.List;
import java.util.Optional;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminAttribute;
import yokohama.lang.ermin.attribute.ErminKey;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminEntity {
    private final ErminName name;

    private final Optional<ErminKey> typeKey;

    private final List<ErminName> entityKeys;

    private final List<ErminAttribute> attributes;
}
