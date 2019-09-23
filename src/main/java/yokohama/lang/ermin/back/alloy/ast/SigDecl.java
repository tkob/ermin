package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Value;

@Value
public class SigDecl implements Paragraph {

    private final boolean abst;

    private final Optional<Mult> mult;

    private final List<String> names;

    private final List<Decl> decls;

    @Override
    public <R> R accept(ParagraphVisitor<R> visitor) {
        return visitor.visitSigDecl(this);
    }

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(StringUtils.repeat(' ', indent));
        if (this.isAbst()) {
            writer.append("abstract ");
        }
        writer.append(mult.map(mult -> mult.toString() + " ").orElse(""));
        writer.append("sig ");
        writer.append(names.stream().collect(Collectors.joining(", ")));
        writer.append(" {");

        boolean firstDecl = true;
        for (Decl decl : decls) {
            if (firstDecl) {
                writer.append('\n');
            } else {
                writer.append(",\n");
            }
            writer.append(StringUtils.repeat(' ', indent + 4));
            decl.writeTo(writer, indent + 4);
            firstDecl = false;
        }
        if (!firstDecl) {
            writer.append('\n');
            writer.append(StringUtils.repeat(' ', indent + 4));
        }
        writer.append("}\n");
    }

    public static SigDecl of(String name, List<Decl> decls) {
        return new SigDecl(false, Optional.empty(), Arrays.asList(name), decls);
    }

    public static SigDecl of(String name, Decl... decls) {
        return SigDecl.of(name, Arrays.asList(decls));
    }
}
