package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class BinOpExpr implements Expr {
    private final Expr left;

    private final BinOp binOp;

    private final Expr right;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append('(');
        left.writeTo(writer, indent);
        switch (binOp) {
            case DOT:
                writer.append(binOp.toString());
                break;
            default:
                writer.append(' ');
                writer.append(binOp.toString());
                writer.append(' ');
        }
        right.writeTo(writer, indent);
        writer.append(')');
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitBinOpExpr(this);
    }

    public static BinOpExpr dot(Expr left, Expr right) {
        return new BinOpExpr(left, BinOp.DOT, right);
    }

    public static BinOpExpr plus(Expr left, Expr right) {
        return new BinOpExpr(left, BinOp.PLUS, right);
    }

    public static BinOpExpr minus(Expr left, Expr right) {
        return new BinOpExpr(left, BinOp.MINUS, right);
    }

    public static Expr or(Expr left, Expr right) {
        return new BinOpExpr(left, BinOp.OR, right);
    }
}
