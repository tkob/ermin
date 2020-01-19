package yokohama.lang.ermin.process;

import lombok.Value;

@Value
public class ErminNotEqualBoolExp implements ErminBoolExp {
    private final ErminExp left;

    private final ErminExp right;

    @Override
    public <R> R accept(ErminBoolExpVisitor<R> visitor) {
        return visitor.visitNotEqualBoolExp(this);
    }

}
