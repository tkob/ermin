package yokohama.lang.ermin.process;

import lombok.Value;
import yokohama.lang.ermin.attribute.ErminName;

@Value
public class ErminInsertStatement implements ErminStatement {
    private final ErminName lvalue;

    private final ErminExp exp;

    @Override
    public <R> R accept(ErminStatementVisitor<R> visitor) {
        return visitor.visitInsertStatement(this);
    }

}
