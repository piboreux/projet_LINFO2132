package compiler.parser.AST;

public class IfNode extends ASTNode {
    private final ASTNode condition, thenBlock, elseBlock;
    public IfNode(ASTNode cond, ASTNode thenB, ASTNode elseB) {
        this.condition = cond; this.thenBlock = thenB; this.elseBlock = elseB;
    }
    @Override
    public String toString(int indent) {
        String res = getIndent(indent) + "If\n" + condition.toString(indent + 1) + thenBlock.toString(indent + 1);
        if (elseBlock != null) res += getIndent(indent) + "Else\n" + elseBlock.toString(indent + 1);
        return res;
    }
}