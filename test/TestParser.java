import static org.junit.Assert.*;
import org.junit.Test;
import java.io.StringReader;
import compiler.Lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.AST.*;
import compiler.parser.AST.literals.*;

public class TestParser {

    // ─── Utilitaire ─────────────────────────────────────────────────────────────

    private ASTNode parse(String input) {
        return new Parser(new Lexer(new StringReader(input))).getAST();
    }

    // ─── Déclarations de variables ──────────────────────────────────────────────

    @Test
    public void testIntVarDecl() {
        ASTNode ast = parse("INT x = 3;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl"));
        assertTrue(out.contains("Type, INT"));
        assertTrue(out.contains("Identifier, x"));
        assertTrue(out.contains("Integer, 3"));
    }

    // grands nombres
    @Test
    public void testBigInt() {
        ASTNode ast = parse("INT big = 999999;");
        assertTrue(ast.toString().contains("Integer, 999999"));
    }

    //floats
    @Test
    public void testFloatVarDecl() {
        ASTNode ast = parse("FLOAT f = 3.14;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl"));
        assertTrue(out.contains("Type, FLOAT"));
        assertTrue(out.contains("Identifier, f"));
    }

    @Test
    public void testStringVarDecl() {
        ASTNode ast = parse("STRING s = \"hello\";");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl"));
        assertTrue(out.contains("Type, STRING"));
    }

    // string vide
    @Test
    public void testEmptyString() {
        ASTNode ast = parse("STRING s = \"\";");
        assertTrue(ast.toString().contains("String, "));
    }

    @Test
    public void testBoolVarDecl() {
        ASTNode ast = parse("BOOL b = true;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl"));
        assertTrue(out.contains("Type, BOOL"));
        assertTrue(out.contains("Identifier, b"));
    }

    @Test
    public void testFinalVarDecl() {
        ASTNode ast = parse("final INT x = 42;");
        String out = ast.toString();
        assertTrue(out.contains("FinalVarDecl"));
        assertTrue(out.contains("Type, INT"));
        assertTrue(out.contains("Integer, 42"));
    }

    // "*" AVAANT "+"
    @Test
    public void testPrioriteMult() {
        ASTNode ast = parse("INT res = 1 + 2 * 3;");
        String out = ast.toString();
        assertTrue(out.contains("ArithmeticOp, +"));
        assertTrue(out.contains("ArithmeticOp, *"));
    }

    //ordre des ops avec parenthèses
    @Test
    public void testPrioriteParentheses() {
        ASTNode ast = parse("INT res = (1 + 2) * 3;");
        String out = ast.toString();
        assertTrue(out.contains("ArithmeticOp, *"));
        assertTrue(out.contains("ArithmeticOp, +"));
    }

    @Test
    public void testLogicalComplex() {
    ASTNode ast = parse("BOOL res = (x == 5) && (y > 10) || not z;");
    String out = ast.toString();
    assertTrue(out.contains("LogicalOp, &&") || out.contains("&&"));
    assertTrue(out.contains("ComparisonOp, ==") || out.contains("=="));
    assertTrue(out.contains("UnaryOp, not") || out.contains("not"));
}

    @Test
    public void testNegatifs() {
        ASTNode ast = parse("INT n = -5;");
        //  Unaryop
        assertTrue(ast.toString().contains("-"));
    }

    @Test
    public void testVarDeclNoInitializer() {
        ASTNode ast = parse("INT a;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl"));
        assertTrue(out.contains("Type, INT"));
        assertTrue(out.contains("Identifier, a"));
    }

    @Test
    public void testCallMultiArgs() {
    ASTNode ast = parse("def INT main() { foo(1, 2, 3); }");
    assertTrue(ast.toString().contains("1"));
    assertTrue(ast.toString().contains("2"));
    assertTrue(ast.toString().contains("3"));
    }

    @Test
    public void testImbricationTotale() {
    String code = "def INT main() { " +
                  "  if (true) { " +
                  "    while (x < 10) { " +
                  "      for (INT i; 0 -> 5; i + 1) { print(i); } " +
                  "    } " +
                  "  } " +
                  "}";
    ASTNode ast = parse(code);
    String out = ast.toString();
    assertTrue(out.contains("If") && out.contains("While") && out.contains("For"));
}

    // ---------------- (IF/WHILE/FOR)---------------
    @Test
    public void testIfElseImbrique() {
        // Un if dans un if, pour tester l'indentation
        ASTNode ast = parse("def main() { if (x == 1) { if (y == 2) { z = 3; } } }");
        String out = ast.toString();
        assertTrue(out.contains("If"));
        // On devrait avoir au moins deux fois "If" dans le texte
        int count = out.split("If").length - 1;
        assertTrue(count >= 2);
    }

    @Test
    public void testWhileSimple() {
        ASTNode ast = parse("def main() { while (true) { x = x + 1; } }");
        assertTrue(ast.toString().contains("While"));
        assertTrue(ast.toString().contains("ArithmeticOp, +"));
    }

    @Test
    public void testForComplexe() {
        // Test de la boucle for avec le range ->
        ASTNode ast = parse("def main() { for (INT i; 0 -> 10; i + 1) { print(i); } }");
        String out = ast.toString();
        assertTrue(out.contains("For"));
    }

    // ─── Déclarations de tableaux ────────────────────────────────────────────────

    @Test
    public void testTableauSimple() {
        ASTNode ast = parse("INT[] tab = INT ARRAY [10];");
        assertTrue(ast.toString().contains("ArrayConstructor"));
    }

    @Test
    public void testAccèsTableau() {
        ASTNode ast = parse("def main() { val = tab[i + 1]; }");
        assertTrue(ast.toString().contains("ArrayAccess"));
    }

    @Test
    public void testCollPoint() {
        ASTNode ast = parse("coll Point { INT x; INT y; }");
        String out = ast.toString();
        assertTrue(out.contains("Point")); 
        assertTrue(out.contains("x"));
        assertTrue(out.contains("y"));
    }

    // ─── Collections ────────────────────────────────────────────────────────────

    @Test
    public void testCollDecl() {
        ASTNode ast = parse("coll Point { INT x; INT y; }");
        String out = ast.toString();
        assertTrue(out.contains("Point"));
        assertTrue(out.contains("x"));
        assertTrue(out.contains("y"));
    }

    @Test
    public void testCollConstructorCall() {
        ASTNode ast = parse("Point p = Point(3, 7);");
        String out = ast.toString();
        assertTrue(out.contains("Call"));
        assertTrue(out.contains("Point"));
    }

    

    // ─── Fonctions ──────────────────────────────────────────────────────────────

    @Test
    public void testFuncDeclVoid() {
        ASTNode ast = parse("def INT main() { }");
        String out = ast.toString();  
        assertTrue(out.contains("main"));
    }

    @Test
    public void testFuncDeclWithReturnType() {
        ASTNode ast = parse("def INT square(INT v) { return v; }");
        String out = ast.toString();
        assertTrue(out.contains("square"));
        assertTrue(out.contains("INT"));
        assertTrue(out.contains("Return"));
    }

    @Test
    public void testReturnExpr() {
        ASTNode ast = parse("def INT foo() { return 42; }");
        String out = ast.toString();
        assertTrue(out.contains("Return"));
        assertTrue(out.contains("Integer, 42"));
    }

    // ─── Structures de contrôle ──────────────────────────────────────────────────

    @Test
    public void testIfStmt() {
        ASTNode ast = parse("def INT main() { if (x == 1) { } }");
        String out = ast.toString();
        assertTrue(out.contains("If"));
        assertFalse(out.contains("Else"));
    }

    @Test
    public void testIfElseStmt() {
        ASTNode ast = parse("def INT main() { if (x == 1) { } else { } }");
        String out = ast.toString();
        assertTrue(out.contains("If"));
        assertTrue(out.contains("Else"));
    }

    @Test
    public void testWhileStmt() {
        ASTNode ast = parse("def INT main() { while (x < 10) { } }");
        String out = ast.toString();
        assertTrue(out.contains("While"));
    }

    @Test
    public void testForStmt() {
        // Test spécifique avec l'opérateur de range "->" du lexer
        ASTNode ast = parse("def INT main() { for (INT i; 1 -> 100; i + 1) { } }");
        String out = ast.toString();
        assertTrue(out.contains("For"));
        assertTrue(out.contains("Type, INT"));
    }
    // ─── Tests d'erreurs (throw une exception) ──────────────────────────

    @Test(expected = RuntimeException.class)
    public void testErreurPointVirgule() {
        // Il manque le ; à la fin
        parse("INT x = 5");
    }

    @Test(expected = RuntimeException.class)
    public void testErreurParenthese() {
        // Parenthèse pas fermée
        parse("if (x == 1 { }");
    }

    @Test(expected = RuntimeException.class)
    public void testMauvaisAssign() {
        parse("x == 5;"); // comparaison, pas une instruction valide seule
    }

    // assignation avec calcul
    @Test
    public void testAssignCalcul() {
        ASTNode ast = parse("INT a = 1 + 2 + 3 + 4;");
        String out = ast.toString();
        assertTrue(out.contains("ArithmeticOp, +"));
    }

}