package compiler.parser.AST.literals;
import compiler.parser.AST.ASTNode;

public class IntLiteralNode extends ASTNode {
    private final String value;

    public IntLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Integer, " + value + "\n";
    }
}