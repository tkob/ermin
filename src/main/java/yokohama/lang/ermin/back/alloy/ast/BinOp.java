package yokohama.lang.ermin.back.alloy.ast;

public enum BinOp {
    OR, DOT, PLUS, MINUS, OVERRIDE, ARROW;

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
            case OVERRIDE:
                return "++";
            case ARROW:
                return "->";
            default:
                throw new IllegalStateException("should never reach here");
        }
    }
}
