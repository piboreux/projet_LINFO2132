package compiler.parser.AST;

public class UnaryOpNode extends ASTNode {
    private final String op;
    private final ASTNode expression;
    public UnaryOpNode(String op, ASTNode expression) {
        this.op = op; this.expression = expression;
    }
    @Override
    public String toString(int indent) {
        return getIndent(indent) + "UnaryOp, " + op + "\n" + expression.toString(indent + 1);
    }
}