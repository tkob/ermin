package yokohama.lang.ermin.type;

import lombok.Value;

@Value
public class ErminClobType implements ErminType {

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitClobType(this);
    }

}
