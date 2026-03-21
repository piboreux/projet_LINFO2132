package compiler.parser.AST.literals;
import compiler.parser.AST.ASTNode;

public class StringLiteralNode extends ASTNode {
    private final String value;
    public StringLiteralNode(String value) { this.value = value; }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "String, " + value + "\n";
    }
}