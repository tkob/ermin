package yokohama.lang.ermin.process;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminNewEntityArgument implements ErminArgument {
    private final String varName;

    private final ErminName entityName;

    @Override
    public <R> R accept(ErminArgumentVisitor<R> visitor) {
        return visitor.visitNewEntityArgument(this);
    }

}
