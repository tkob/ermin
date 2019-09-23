package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import lombok.Value;

@Value
public class LetExpr implements Expr {
    private final List<LetDecl> letDecls;

    private final Block block;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append("let ");
        boolean first = true;
        for (LetDecl letDecl : letDecls) {
            if (!first) {
                writer.append(", ");
            }
            letDecl.writeTo(writer, indent);
            first = false;
        }
        writer.append(" ");
        block.writeTo(writer, indent + 4);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitLetExpr(this);
    }

}
