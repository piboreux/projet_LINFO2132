package compiler.parser.AST;

public abstract class ASTNode {
    // Méthode abstraite pour l'affichage récursif
    public abstract String toString(int indent);

    // Utilitaire pour l'indentation uniforme
    protected String getIndent(int n) {
        return "  ".repeat(n);
    }

    @Override
    public String toString() {
        return toString(0);
    }
}