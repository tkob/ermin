package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import lombok.Value;

@Value
public class AlloyModule implements ToString {
    private final Collection<Paragraph> paragraphs;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        writer.append("open util/ordering[State]\n\n");

        boolean first = true;
        for (Paragraph paragraph : paragraphs) {
            if (!first) {
                writer.append('\n');
            }
            paragraph.writeTo(writer, indent);
            first = false;
        }
        writer.append("\npred show {}\n\nrun show\n");
    }
}
