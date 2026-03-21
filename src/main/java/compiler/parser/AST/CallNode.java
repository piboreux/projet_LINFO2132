package compiler.parser.AST;
import java.util.List;

public class CallNode extends ASTNode {
    private final String name;
    private final List<ASTNode> arguments;
    public CallNode(String name, List<ASTNode> arguments) {
        this.name = name; this.arguments = arguments;
    }
    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "Call, " + name + "\n");
        for (ASTNode arg : arguments) sb.append(arg.toString(indent + 1));
        return sb.toString();
    }
}