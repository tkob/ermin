package yokohama.lang.ermin.back.alloy.ast;

public enum Mult {
    LONE, ONE, SOME;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
