package yokohama.lang.ermin.back.alloy.ast;

public enum BinOp {
    OR, DOT, PLUS, MINUS;

    public String toString() {
        switch (this) {
            case OR:
                return "or";
            case DOT:
                return ".";
            case PLUS:
                return "+";
            case MINUS:
                return "-";
            default:
                throw new IllegalStateException("should never reach here");
        }
    }
}
