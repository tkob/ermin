package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import lombok.Lombok;
import lombok.Value;

@Value
public class CmdDecl implements Paragraph {

    private final Optional<String> name;

    private final CmdVerb cmdVerb;

    private final QualNameOrBlock qualNameOrBlock;

    private final Optional<Scope> scope;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append(name.map(n -> n + ": ").orElse(""));
        writer.append(cmdVerb.toString());
        writer.append(' ');
        qualNameOrBlock.writeTo(writer, indent);
        writer.append(' ');
        scope.ifPresent(s -> {
            try {
                s.writeTo(writer, indent);
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        writer.append('\n');
    }

    @Override
    public <R> R accept(ParagraphVisitor<R> visitor) {
        return visitor.visitCmdDecl(this);
    }

    public static CmdDecl check(String name) {
        return new CmdDecl(Optional.empty(), CmdVerb.CHECK, QualName.of(name), Optional.empty());
    }

    public static CmdDecl check(String name, Scope scope) {
        return new CmdDecl(Optional.empty(), CmdVerb.CHECK, QualName.of(name), Optional.of(scope));
    }
}
