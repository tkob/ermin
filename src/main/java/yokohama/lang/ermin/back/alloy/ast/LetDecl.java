package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class LetDecl implements ToString {
    private final String name;

    private final Expr expr;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(name);
        writer.append(" = ");
        expr.writeTo(writer, indent);
    }
}
