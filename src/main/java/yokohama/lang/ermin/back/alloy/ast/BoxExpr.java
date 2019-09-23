package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import lombok.Value;

@Value
public class BoxExpr implements Expr {
    private final Expr expr;

    private final List<Expr> exprs;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        expr.writeTo(writer, indent);
        writer.append(" [");
        boolean first = true;
        for (Expr expr : exprs) {
            if (!first) {
                writer.append(", ");
            }
            expr.writeTo(writer, indent);
            first = false;
        }
        writer.append("]");
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitBoxExpr(this);
    }

}
