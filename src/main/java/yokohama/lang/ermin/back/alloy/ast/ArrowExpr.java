package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import lombok.Value;

@Value
public class ArrowExpr implements Expr {

    private final Expr leftExpr;

    private final Optional<Mult> leftMult;

    private final Optional<Mult> rightMult;

    private final Expr rightExpr;

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitArrowExpr(this);
    }

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        leftExpr.writeTo(writer, indent);
        writer.append(' ');
        writer.append(leftMult.map(mult -> mult.toString() + " ").orElse(""));
        writer.append("-> ");
        writer.append(rightMult.map(mult -> mult.toString() + " ").orElse(""));
        rightExpr.writeTo(writer, indent);
    }

    public static ArrowExpr of(Expr leftExpr, Expr rightExpr) {
        return new ArrowExpr(leftExpr, Optional.empty(), Optional.empty(), rightExpr);
    }
}
