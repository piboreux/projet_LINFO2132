package compiler.parser.AST;

public class VarDeclNode extends ASTNode {
    private final String type;
    private final String name;
    private final boolean isFinal;
    private final ASTNode initialValue;

    public VarDeclNode(String type, String name, boolean isFinal, ASTNode initialValue) {
        this.type = type; this.name = name; this.isFinal = isFinal; this.initialValue = initialValue;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public boolean isFinal() { return isFinal; }
    public ASTNode getInitializer() { return initialValue; }

    @Override
    public String toString(int indent) {
        String res = getIndent(indent) + (isFinal ? "FinalVarDecl" : "VarDecl") + "\n";
        res += getIndent(indent + 1) + "Type, " + type + "\n";
        res += getIndent(indent + 1) + "Identifier, " + name + "\n";
        if (initialValue != null) {
            res += initialValue.toString(indent + 1);
        }
        return res;
    }
}