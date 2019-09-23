package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class UnOpExpr implements Expr {
    private final UnOp unOp;

    private final Expr expr;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(unOp.toString());
        writer.append(' ');
        expr.writeTo(writer, indent);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitUnOpExpr(this);
    }

}
