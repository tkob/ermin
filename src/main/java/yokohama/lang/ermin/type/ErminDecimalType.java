package yokohama.lang.ermin.type;

import java.util.Optional;

import lombok.Value;

@Value
public class ErminDecimalType implements ErminType {

    private final Optional<Integer> precision;

    private final Optional<Integer> scale;

    public ErminDecimalType() {
        this.precision = Optional.empty();
        this.scale = Optional.empty();
    }

    public ErminDecimalType(int precision) {
        this.precision = Optional.of(precision);
        this.scale = Optional.empty();
    }

    public ErminDecimalType(int precision, int scale) {
        this.precision = Optional.of(precision);
        this.scale = Optional.of(scale);
    }

    @Override
    public <R> R accept(ErminTypeVisitor<R> visitor) {
        return visitor.visitDecimalType(this);
    }

}
