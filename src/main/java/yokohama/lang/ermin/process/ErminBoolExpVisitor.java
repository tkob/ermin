package yokohama.lang.ermin.process;

public interface ErminBoolExpVisitor<R> {
    R visitUniversalBoolExp(ErminUniversalBoolExp universalBoolExp);

    R visitEqualBoolExp(ErminEqualBoolExp equalBoolExp);

    R visitNotEqualBoolExp(ErminNotEqualBoolExp notEqualBoolExp);

}
