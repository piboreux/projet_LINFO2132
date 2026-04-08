package compiler.parser.AST;
import java.util.List;

public class CollDeclNode extends ASTNode {
    private final String name;
    private final List<VarDeclNode> fields;
    public CollDeclNode(String name, List<VarDeclNode> fields) {
        this.name = name; this.fields = fields;
    }
    public String getName() { return name; }
    public List<VarDeclNode> getFields() { return fields; }
    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "CollDecl, " + name + "\n");
        for (VarDeclNode f : fields) sb.append(f.toString(indent + 1));
        return sb.toString();
    }
}