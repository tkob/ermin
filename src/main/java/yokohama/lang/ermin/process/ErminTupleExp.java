package yokohama.lang.ermin.process;

import java.util.List;

import lombok.Value;

@Value
public class ErminTupleExp implements ErminExp {
    private final List<ErminExp> exps;

    @Override
    public <R> R accept(ErminExpVisitor<R> visitor) {
        return visitor.visitTupleExp(this);
    }

}
