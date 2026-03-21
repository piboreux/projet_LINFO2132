package compiler.parser.AST.literals;
import compiler.parser.AST.ASTNode;

public class IdentifierNode extends ASTNode {
    private final String name;
    public IdentifierNode(String name) { this.name = name; }
    public String getName() { return name; }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Identifier, " + name + "\n";
    }
}