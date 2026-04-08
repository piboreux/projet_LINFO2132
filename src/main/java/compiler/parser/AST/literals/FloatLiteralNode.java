package compiler.parser.AST.literals;
import compiler.parser.AST.ASTNode;

public class FloatLiteralNode extends ASTNode {
    private final String value;

    public FloatLiteralNode(String value) { this.value = value; }
    public String getValue() { return value; }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Float, " + value + "\n";
    }
}