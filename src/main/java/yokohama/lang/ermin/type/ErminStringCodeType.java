package yokohama.lang.ermin.type;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminStringCodeType implements ErminType {
    private final ErminName name;

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitStringCodeType(this);
    }

}
