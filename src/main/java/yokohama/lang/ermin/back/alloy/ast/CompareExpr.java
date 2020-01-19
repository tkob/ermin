package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class CompareExpr implements Expr {
    private final Expr left;

    private final boolean not;

    private final CompareOp compareOp;

    private final Expr right;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        left.writeTo(writer, indent);
        writer.append(' ');
        if (not) {
            writer.append("not ");
        }
        writer.append(compareOp.toString());
        writer.append(' ');
        right.writeTo(writer, indent);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitCompareExpr(this);
    }

    public static CompareExpr in(Expr left, Expr right) {
        return new CompareExpr(left, false, CompareOp.IN, right);
    }

    public static CompareExpr notIn(Expr left, Expr right) {
        return new CompareExpr(left, true, CompareOp.IN, right);
    }

    public static CompareExpr eq(Expr left, Expr right) {
        return new CompareExpr(left, false, CompareOp.EQ, right);
    }

    public static CompareExpr notEq(Expr left, Expr right) {
        return new CompareExpr(left, true, CompareOp.EQ, right);
    }
}
