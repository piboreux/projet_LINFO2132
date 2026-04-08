package compiler.parser.AST;

public class ForNode extends ASTNode {
    private final ASTNode iterator;
    private final ASTNode start, end, increment, body;

    public ForNode(ASTNode iter, ASTNode s, ASTNode e, ASTNode inc, ASTNode b) {
        this.iterator = iter; this.start = s; this.end = e; this.increment = inc; this.body = b;
    }

    public VarDeclNode getLoopVar() { return iterator; }
    public ASTNode getStart() { return start; }
    public ASTNode getEnd() { return end; }
    public ASTNode getIncrement() { return increment; }
    public ASTNode getBody() { return body; }
    public ASTNode getCondition() {
        return null;
    }

    @Override
    public String toString(int indent) {
        return getIndent(indent) + "For\n"
                + iterator.toString(indent + 1)
                + getIndent(indent+1) + "Range\n"
                + start.toString(indent + 2)
                + end.toString(indent + 2)
                + getIndent(indent+1) + "Increment\n"
                + increment.toString(indent + 2)
                + body.toString(indent + 1);
    }
}