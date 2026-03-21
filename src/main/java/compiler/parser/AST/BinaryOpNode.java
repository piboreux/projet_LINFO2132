package compiler.parser.AST;

public class BinaryOpNode extends ASTNode {
    private final String operator;
    private final ASTNode left;
    private final ASTNode right;

    public BinaryOpNode(String operator, ASTNode left, ASTNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() { return operator; }
    public ASTNode getLeft()    { return left; }
    public ASTNode getRight()   { return right; }

    @Override
    public String toString(int indent) {
        String label;
        switch (operator) {
            case "+": case "-": case "*": case "/": case "%":
                label = "ArithmeticOp, " + operator;
                break;
            case "==": case "=/=": case "<": case ">": case "<=": case ">=":
                label = "ComparisonOp, " + operator;
                break;
            case "&&": case "||":
                label = "LogicalOp, " + operator;
                break;
            default:
                label = "BinaryOp, " + operator;
        }
        return getIndent(indent) + label + "\n"
                + left.toString(indent + 1)
                + right.toString(indent + 1);
    }
}