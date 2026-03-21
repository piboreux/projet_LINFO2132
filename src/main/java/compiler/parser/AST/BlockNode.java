package compiler.parser.AST;
import java.util.List;

public class BlockNode extends ASTNode {
    private final List<ASTNode> statements;
    public BlockNode(List<ASTNode> statements) { this.statements = statements; }
    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "Block\n");
        for (ASTNode s : statements) sb.append(s.toString(indent + 1));
        return sb.toString();
    }
}