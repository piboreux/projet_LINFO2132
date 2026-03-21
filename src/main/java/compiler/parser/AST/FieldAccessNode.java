package compiler.parser.AST;

public class FieldAccessNode extends ASTNode {
    private final ASTNode object;
    private final String field;
    public FieldAccessNode(ASTNode object, String field) {
        this.object = object; this.field = field;
    }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "FieldAccess, " + field + "\n" + object.toString(indent + 1);
    }
}