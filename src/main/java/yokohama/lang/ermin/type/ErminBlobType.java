package yokohama.lang.ermin.type;

import lombok.Value;

@Value
public class ErminBlobType implements ErminType {

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitBlobType(this);
    }

}
