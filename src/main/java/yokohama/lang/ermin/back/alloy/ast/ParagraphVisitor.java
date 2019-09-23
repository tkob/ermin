package yokohama.lang.ermin.back.alloy.ast;

public interface ParagraphVisitor<R> {
    R visitSigDecl(SigDecl sigDecl);

    R visitAssertDecl(AssertDecl assertDecl);

    R visitFactDecl(FactDecl factDecl);

    R visitCmdDecl(CmdDecl cmdDecl);

    R visitPredDecl(PredDecl predDecl);
}
