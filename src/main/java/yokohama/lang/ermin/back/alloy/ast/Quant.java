package yokohama.lang.ermin.back.alloy.ast;

public enum Quant {
    ALL, NO, SUM, LONE, ONE, SOME;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
