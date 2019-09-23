package yokohama.lang.ermin.back.alloy.ast;

public enum CmdVerb {
    RUN, CHECK;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
