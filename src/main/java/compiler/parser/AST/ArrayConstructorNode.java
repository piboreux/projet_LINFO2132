package compiler.parser.AST;

public class ArrayConstructorNode extends ASTNode {
    private final String elementType;
    private final ASTNode size;

    public ArrayConstructorNode(String elementType, ASTNode size) {
        this.elementType = elementType;
        this.size = size;
    }

    @Override
    public String toString(int indent) {
        return getIndent(indent) + "ArrayConstructor, " + elementType + "\n"
                + size.toString(indent + 1);
    }
}