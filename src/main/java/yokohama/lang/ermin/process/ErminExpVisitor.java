package yokohama.lang.ermin.process;

public interface ErminExpVisitor<R> {
    R visitVarExp(ErminVarExp varExp);

}
