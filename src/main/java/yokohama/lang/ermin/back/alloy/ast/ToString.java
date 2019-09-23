package yokohama.lang.ermin.back.alloy.ast;

import java.io.IOException;
import java.io.Writer;

public interface ToString {
    void writeTo(Writer writer, int indent) throws IOException;
}
