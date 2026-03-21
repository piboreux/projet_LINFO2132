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
        assertTrue(out.contains("VarDecl, INT x"));
        assertTrue(out.contains("Integer, 3"));
    }

    @Test
    public void testFloatVarDecl() {
        ASTNode ast = parse("FLOAT f = 3.14;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, FLOAT f"));
        assertTrue(out.contains("Float, 3.14"));
    }

    @Test
    public void testStringVarDecl() {
        ASTNode ast = parse("STRING s = \"hello\";");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, STRING s"));
        assertTrue(out.contains("String, hello"));
    }

    @Test
    public void testBoolVarDecl() {
        ASTNode ast = parse("BOOL b = true;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, BOOL b"));
        assertTrue(out.contains("Boolean, true"));
    }

    @Test
    public void testFinalVarDecl() {
        ASTNode ast = parse("final INT x = 42;");
        String out = ast.toString();
        assertTrue(out.contains("FinalVarDecl, INT x"));
        assertTrue(out.contains("Integer, 42"));
    }

    @Test
    public void testVarDeclNoInitializer() {
        ASTNode ast = parse("INT a;");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, INT a"));
    }

    // ─── Déclarations de tableaux ────────────────────────────────────────────────

    @Test
    public void testArrayDecl() {
        ASTNode ast = parse("INT[] c = INT ARRAY [5];");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, INT[] c"));
        assertTrue(out.contains("ArrayConstructor, INT"));
        assertTrue(out.contains("Integer, 5"));
    }

    @Test
    public void testArrayAccess() {
        ASTNode ast = parse("def main() { INT x = a[2]; }");
        String out = ast.toString();
        assertTrue(out.contains("ArrayAccess"));
        assertTrue(out.contains("Integer, 2"));
    }

    // ─── Collections ────────────────────────────────────────────────────────────

    @Test
    public void testCollDecl() {
        ASTNode ast = parse("coll Point { INT x; INT y; }");
        String out = ast.toString();
        assertTrue(out.contains("CollDecl, Point"));
        assertTrue(out.contains("VarDecl, INT x"));
        assertTrue(out.contains("VarDecl, INT y"));
    }

    @Test
    public void testCollDeclWithArrayField() {
        ASTNode ast = parse("coll Person { STRING name; INT[] history; }");
        String out = ast.toString();
        assertTrue(out.contains("CollDecl, Person"));
        assertTrue(out.contains("VarDecl, INT[] history"));
    }

    @Test
    public void testCollConstructorCall() {
        ASTNode ast = parse("Point p = Point(3, 7);");
        String out = ast.toString();
        assertTrue(out.contains("VarDecl, Point p"));
        assertTrue(out.contains("Call, Point"));
    }

    @Test
    public void testFieldAccess() {
        ASTNode ast = parse("def main() { INT x = p.x; }");
        String out = ast.toString();
        assertTrue(out.contains("FieldAccess, x"));
    }

    // ─── Fonctions ──────────────────────────────────────────────────────────────

    @Test
    public void testFuncDeclVoid() {
        ASTNode ast = parse("def main() { }");
        String out = ast.toString();
        assertTrue(out.contains("FuncDecl, main : void"));
    }

    @Test
    public void testFuncDeclWithReturnType() {
        ASTNode ast = parse("def INT square(INT v) { return v; }");
        String out = ast.toString();
        assertTrue(out.contains("FuncDecl, square : INT"));
        assertTrue(out.contains("VarDecl, INT v"));
        assertTrue(out.contains("Return"));
    }

    @Test
    public void testFuncDeclMultipleParams() {
        ASTNode ast = parse("def INT add(INT a, INT b) { return a; }");
        String out = ast.toString();
        assertTrue(out.contains("FuncDecl, add : INT"));
        assertTrue(out.contains("VarDecl, INT a"));
        assertTrue(out.contains("VarDecl, INT b"));
    }

    @Test
    public void testFuncCallAsStatement() {
        ASTNode ast = parse("def main() { println(42); }");
        String out = ast.toString();
        assertTrue(out.contains("Call, println"));
    }

    @Test
    public void testReturnVoid() {
        ASTNode ast = parse("def main() { return; }");
        String out = ast.toString();
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
        ASTNode ast = parse("def main() { if (x) { } }");
        String out = ast.toString();
        assertTrue(out.contains("If"));
        assertFalse(out.contains("Else"));
    }

    @Test
    public void testIfElseStmt() {
        ASTNode ast = parse("def main() { if (x) { } else { } }");
        String out = ast.toString();
        assertTrue(out.contains("If"));
        assertTrue(out.contains("Else"));
    }

    @Test
    public void testWhileStmt() {
        ASTNode ast = parse("def main() { while (x) { } }");
        String out = ast.toString();
        assertTrue(out.contains("While"));
    }

    @Test
    public void testForStmt() {
        ASTNode ast = parse("def main() { for (INT i; 1 -> 100; i+1) { } }");
        String out = ast.toString();
        assertTrue(out.contains("For"));
        assertTrue(out.contains("VarDecl, INT i"));
        assertTrue(out.contains("Range"));
        assertTrue(out.contains("Increment"));
    }

    // ─── Expressions et précédence ───────────────────────────────────────────────

    @Test
    public void testArithmeticPrecedence() {
        // 1 + 2 * 3 : le nœud racine doit être +, donc + apparaît à indent=2, * à indent=3
        ASTNode ast = parse("INT x = 1 + 2 * 3;");
        String out = ast.toString();
        // La ligne avec + doit avoir moins d'indentation que la ligne avec *
        String[] lines = out.split("\n");
        int plusIndent = -1, timesIndent = -1;
        for (String line : lines) {
            if (line.contains("ArithmeticOp, +") && plusIndent == -1)
                plusIndent = countLeadingSpaces(line);
            if (line.contains("ArithmeticOp, *") && timesIndent == -1)
                timesIndent = countLeadingSpaces(line);
        }
        assertTrue("'+' doit être moins indenté que '*'", plusIndent < timesIndent);
    }

    @Test
    public void testParenthesesOverridePrecedence() {
        // (1 + 2) * 3 : le nœud racine doit être *, donc * moins indenté que +
        ASTNode ast = parse("INT x = (1 + 2) * 3;");
        String out = ast.toString();
        String[] lines = out.split("\n");
        int plusIndent = -1, timesIndent = -1;
        for (String line : lines) {
            if (line.contains("ArithmeticOp, +") && plusIndent == -1)
                plusIndent = countLeadingSpaces(line);
            if (line.contains("ArithmeticOp, *") && timesIndent == -1)
                timesIndent = countLeadingSpaces(line);
        }
        assertTrue("'*' doit être moins indenté que '+'", timesIndent < plusIndent);
    }
    private int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') count++;
        return count;
    }

    @Test
    public void testUnaryMinus() {
        ASTNode ast = parse("INT x = -5;");
        String out = ast.toString();
        assertTrue(out.contains("UnaryOp, -"));
    }

    @Test
    public void testUnaryNot() {
        ASTNode ast = parse("BOOL b = not true;");
        String out = ast.toString();
        assertTrue(out.contains("UnaryOp, not"));
    }

    @Test
    public void testComparisonOp() {
        ASTNode ast = parse("BOOL b = x > 10;");
        String out = ast.toString();
        assertTrue(out.contains("ComparisonOp, >"));
    }

    @Test
    public void testEqualityOp() {
        ASTNode ast = parse("BOOL b = x == 3;");
        String out = ast.toString();
        assertTrue(out.contains("ComparisonOp, =="));
    }

    @Test
    public void testNeqOp() {
        ASTNode ast = parse("BOOL b = x =/= 3;");
        String out = ast.toString();
        assertTrue(out.contains("ComparisonOp, =/="));
    }

    @Test
    public void testLogicalAnd() {
        ASTNode ast = parse("BOOL b = x && y;");
        String out = ast.toString();
        assertTrue(out.contains("LogicalOp, &&"));
    }

    @Test
    public void testLogicalOr() {
        ASTNode ast = parse("BOOL b = x || y;");
        String out = ast.toString();
        assertTrue(out.contains("LogicalOp, ||"));
    }

    @Test
    public void testAssignment() {
        ASTNode ast = parse("def main() { x = 5; }");
        String out = ast.toString();
        assertTrue(out.contains("Assignment"));
        assertTrue(out.contains("Identifier, x"));
        assertTrue(out.contains("Integer, 5"));
    }

    @Test
    public void testArrayAssignment() {
        ASTNode ast = parse("def main() { a[3] = 1234; }");
        String out = ast.toString();
        assertTrue(out.contains("Assignment"));
        assertTrue(out.contains("ArrayAccess"));
        assertTrue(out.contains("Integer, 1234"));
    }

    @Test
    public void testFieldAssignment() {
        ASTNode ast = parse("def main() { a.x = 123; }");
        String out = ast.toString();
        assertTrue(out.contains("Assignment"));
        assertTrue(out.contains("FieldAccess, x"));
    }

    // ─── Erreurs syntaxiques ─────────────────────────────────────────────────────

    @Test
    public void testMissingSemicolon() {
        assertThrows(RuntimeException.class, () -> parse("INT x = 3"));
    }

    @Test
    public void testMissingAssign() {
        assertThrows(RuntimeException.class, () -> parse("INT x 3;"));
    }

    @Test
    public void testUnclosedBrace() {
        assertThrows(RuntimeException.class, () -> parse("def main() {"));
    }

    @Test
    public void testUnclosedParen() {
        assertThrows(RuntimeException.class, () -> parse("def main( { }"));
    }

    @Test
    public void testEmptyExpression() {
        assertThrows(RuntimeException.class, () -> parse("INT x = ;"));
    }
}