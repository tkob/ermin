package yokohama.lang.ermin.type;

import lombok.Value;

@Value
public class ErminStringCodeType implements ErminType {
    private final String name;

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitStringCodeType(this);
    }

}
