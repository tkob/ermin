package yokohama.lang.ermin.relationship;

public interface ErminRelationshipExp {
    <R> R accept(ErminRelationshipExpVisitor<R> visitor);
}
