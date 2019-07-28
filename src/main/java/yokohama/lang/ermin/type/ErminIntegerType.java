package yokohama.lang.ermin.type;

import lombok.Value;

@Value
public class ErminIntegerType implements ErminType {

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitIntegerType(this);
    }

}
