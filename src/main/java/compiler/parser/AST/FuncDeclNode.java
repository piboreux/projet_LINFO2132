package compiler.parser.AST;
import java.util.List;

public class FuncDeclNode extends ASTNode {
    private final String name, returnType;
    private final List<VarDeclNode> params;
    private final ASTNode body;

    public FuncDeclNode(String name, String ret, List<VarDeclNode> p, ASTNode b) {
        this.name = name; this.returnType = ret; this.params = p; this.body = b;
    }
    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "FuncDecl, " + name + " : " + returnType + "\n");
        for (VarDeclNode p : params) sb.append(p.toString(indent + 1));
        sb.append(body.toString(indent + 1));
        return sb.toString();
    }
}