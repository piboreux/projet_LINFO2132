package compiler.parser.AST;

public class ParamNode extends ASTNode {
    private final String type;
    private final String name;

    public ParamNode(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() { return type; }
    public String getName() { return name; }

    @Override
    public String toString(int indent) {
        return getIndent(indent) + "Param, " + type + " " + name + "\n";
    }
}
