package yokohama.lang.ermin.back.alloy.ast;

public enum CompareOp {
    IN, EQ, GT, LT, GTE, LTE;

    public String toString() {
        switch (this) {
            case IN:
                return "in";
            case EQ:
                return "=";
            case GT:
                return ">";
            case LT:
                return "<";
            case GTE:
                return ">=";
            case LTE:
                return "<=";
            default:
                throw new IllegalStateException("should never reach here");
        }
    }
}
