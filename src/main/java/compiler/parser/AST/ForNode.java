package compiler.parser.AST;

public class ForNode extends ASTNode {
    private final ASTNode iterator;
    private final ASTNode start, end, increment, body;

    public ForNode(ASTNode iter, ASTNode s, ASTNode e, ASTNode inc, ASTNode b) {
        this.iterator = iter; this.start = s; this.end = e; this.increment = inc; this.body = b;
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