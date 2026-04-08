package compiler.parser.AST;
import java.util.List;

public class ProgramNode extends ASTNode {
    private final List<ASTNode> declarations;
    public ProgramNode(List<ASTNode> decls) { this.declarations = decls; }

    public List<ASTNode> getChildren() { return declarations; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "Program\n");
        for (ASTNode d : declarations) sb.append(d.toString(indent + 1));
        return sb.toString();
    }
}