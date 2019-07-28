package yokohama.lang.ermin.type;

import lombok.Value;

@Value
public class ErminCharType implements ErminType {

    private int size;

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitCharType(this);
    }

}
