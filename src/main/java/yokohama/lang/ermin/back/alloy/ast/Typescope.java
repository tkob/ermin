package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

import lombok.Value;

@Value
public class Typescope implements ToString {
    private final boolean exactly;

    private final int number;

    private final QualName qualName;

    @Override
    public void writeTo(Writer writer, int indent) throws IOException {
        if (exactly) {
            writer.append("exactly ");
        }
        writer.append(Integer.toString(number));
        writer.append(' ');
        qualName.writeTo(writer, indent);
    }
}
