package compiler.parser.AST;

public class ReturnNode extends ASTNode {
    private final ASTNode expression;
    public ReturnNode(ASTNode expr) { this.expression = expr; }
    @Override
    public String toString(int indent) {
        String res = getIndent(indent) + "Return\n";
        if (expression != null) res += expression.toString(indent + 1);
        return res;
    }
}