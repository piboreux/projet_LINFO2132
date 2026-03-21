package compiler.parser.AST;

public class WhileNode extends ASTNode {
    private final ASTNode condition, body;
    public WhileNode(ASTNode cond, ASTNode body) { this.condition = cond; this.body = body; }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "While\n" + condition.toString(indent + 1) + body.toString(indent + 1);
    }
}