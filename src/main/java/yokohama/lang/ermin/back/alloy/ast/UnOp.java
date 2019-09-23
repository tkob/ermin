package yokohama.lang.ermin.back.alloy.ast;

public enum UnOp {
    NO, LONE, ONE, SOME, SET;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
