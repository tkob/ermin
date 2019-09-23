package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class QuantExpr implements Expr {
    private final Quant quant;

    private final List<Decl> decls;

    private final List<Expr> exprs;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(quant.toString());
        writer.append(' ');
        boolean first = true;
        for (Decl decl : decls) {
            if (!first) {
                writer.append(", ");
            }
            decl.writeTo(writer, indent);
            first = false;
        }
        if (exprs.size() == 1) {
            writer.append(" | ");
            exprs.get(0).writeTo(writer, indent);
        } else {
            writer.append(" {\n");
            writer.append(StringUtils.repeat(' ', indent + 4));
            for (Expr expr : exprs) {
                expr.writeTo(writer, indent + 4);
                writer.append("\n");
            }
            writer.append("}");
        }
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitQuantExpr(this);
    }

    public static QuantExpr of(Quant quant, String name, Expr expr, Expr... exprs) {
        return new QuantExpr(quant, Arrays.asList(Decl.of(name, expr)), Arrays.asList(exprs));
    }

    public static QuantExpr all(String name, Expr expr, Expr... exprs) {
        return QuantExpr.of(Quant.ALL, name, expr, exprs);
    }

    public static QuantExpr some(String name, Expr expr, Expr... exprs) {
        return QuantExpr.of(Quant.SOME, name, expr, exprs);
    }

}
