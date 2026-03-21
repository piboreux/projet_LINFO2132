package compiler.parser.AST.literals;
import compiler.parser.AST.ASTNode;

public class BoolLiteralNode extends ASTNode {
    private final String value;
    public BoolLiteralNode(String value) { this.value = value; }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Boolean, " + value + "\n";
    }
}