package yokohama.lang.ermin.process;

import java.util.List;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminUpdateStatement implements ErminStatement {
    private final ErminName lvalue;

    private final List<ErminExp> exps;

    @Override
    public <R> R accept(ErminStatementVisitor<R> visitor) {
        return visitor.visitUpdateStatement(this);
    }

}
