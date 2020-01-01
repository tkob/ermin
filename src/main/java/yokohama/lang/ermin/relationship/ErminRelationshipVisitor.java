package yokohama.lang.ermin.relationship;

public interface ErminRelationshipVisitor<R> {
    R visitBinaryRelationship(ErminBinaryRelationship binaryRelationship);

    R visitMultiRelationship(ErminMultiRelationship multiRelationship);
}
