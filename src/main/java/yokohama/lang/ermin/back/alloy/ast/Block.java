package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class Block implements ToString, QualNameOrBlock {
    private final List<Expr> exprs;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append("{\n");
        for (Expr expr : exprs) {
            writer.append(StringUtils.repeat(' ', indent));
            expr.writeTo(writer, indent);
            writer.append("\n");
        }
        writer.append(StringUtils.repeat(' ', indent));
        writer.append("}\n");
    }

    @Override
    public <R> R accept(QualNameOrBlockVisitor<R> visitor) {
        return visitor.visitBlock(this);
    }

    public static Block of(Expr... exprs) {
        return new Block(Arrays.asList(exprs));
    }
}
