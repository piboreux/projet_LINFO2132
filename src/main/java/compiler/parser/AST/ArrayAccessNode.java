package compiler.parser.AST;

public class ArrayAccessNode extends ASTNode {
    private final ASTNode array;
    private final ASTNode index;
    public ArrayAccessNode(ASTNode array, ASTNode index) {
        this.array = array; this.index = index;
    }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "ArrayAccess\n" +
                array.toString(indent + 1) + index.toString(indent + 1);
    }
}