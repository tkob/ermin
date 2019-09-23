package yokohama.lang.ermin.back.alloy.ast;

public interface Paragraph extends ToString {
    <R> R accept(ParagraphVisitor<R> visitor);
}
