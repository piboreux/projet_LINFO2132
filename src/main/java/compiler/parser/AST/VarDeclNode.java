package compiler.parser.AST;

public class VarDeclNode extends ASTNode {
    private final String type;
    private final String name;
    private final boolean isFinal;
    private final ASTNode initialValue;

    public VarDeclNode(String type, String name, boolean isFinal, ASTNode initialValue) {
        this.type = type; this.name = name; this.isFinal = isFinal; this.initialValue = initialValue;
    }
    @Override
    public String toString(int indent) {
        String res = getIndent(indent) + (isFinal ? "FinalVarDecl, " : "VarDecl, ") + type + " " + name + "\n";
        if (initialValue != null) res += initialValue.toString(indent + 1);
        return res;
    }
}