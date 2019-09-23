package yokohama.lang.ermin;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

import org.junit.Test;

import yokohama.lang.ermin.back.alloy.AlloyTranslator;
import yokohama.lang.ermin.back.alloy.ast.AlloyModule;
import yokohama.lang.ermin.back.reladomo.ReladomoTranslator;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;

public class AlloyTest {
    FrontEndProcessor frontEndProcessor = new FrontEndProcessor();

    ReladomoTranslator reladomoTranslator = new ReladomoTranslator();

    AlloyTranslator alloyTranslator = new AlloyTranslator();

    private void spelloutAlloy(ErminTuple erminTuple, PrintStream out) throws IOException {
        final AlloyModule alloyModule = alloyTranslator.toAlloyModule(erminTuple, 4);
        final Writer writer = new OutputStreamWriter(out);
        alloyModule.writeTo(writer, 0);
        writer.flush();
    }

    @Test
    public void test() throws Exception {
        final ErminTuple erminTuple =
            frontEndProcessor.process(this.getClass().getResourceAsStream("/bts.ermin"));

        spelloutAlloy(erminTuple, System.out);
    }

}
