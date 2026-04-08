package compiler.parser.AST;

public class AssignNode extends ASTNode {
    private final ASTNode target;
    private final ASTNode value;
    public AssignNode(ASTNode target, ASTNode value) {
        this.target = target; this.value = value;
    }

    public ASTNode getTarget() { return target; }
    public ASTNode getValue() { return value; }

    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Assignment\n" +
                target.toString(indent + 1) + value.toString(indent + 1);
    }
}