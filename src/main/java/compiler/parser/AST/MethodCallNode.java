package compiler.parser.AST;

import java.util.List;

public class MethodCallNode extends ASTNode {
    private final ASTNode object;
    private final String method;
    private final List<ASTNode> args;

    public MethodCallNode(ASTNode object, String method, List<ASTNode> args) {
        this.object = object;
        this.method = method;
        this.args = args;
    }

    public ASTNode getObject() { return object; }
    public String getMethod() { return method; }
    public List<ASTNode> getArgs() { return args; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent)).append("MethodCall, ").append(method).append("\n");
        sb.append(object.toString(indent + 1));
        for (ASTNode arg : args) sb.append(arg.toString(indent + 1));
        return sb.toString();
    }
}
