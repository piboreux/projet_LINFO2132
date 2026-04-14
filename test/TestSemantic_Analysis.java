
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.StringReader;
import compiler.Lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.AST.*;
import compiler.Semantic_Analysis.Semantic_Analysis;

public class TestSemantic_Analysis {

    private ASTNode parse(String input) {
        return new Parser(new Lexer(new StringReader(input))).getAST();
    }

    private void analyzeExpectingError(String input, String expectedKeyword) {
        try {
            ASTNode ast = parse(input);
            Semantic_Analysis analyzer = new Semantic_Analysis();
            if (ast instanceof ProgramNode program) {
                analyzer.analyze(program);
            }
            fail("L'erreur " + expectedKeyword + " aurait dû être levée.");
        } catch (RuntimeException e) {
            assertTrue("Le message devrait contenir " + expectedKeyword, 
                       e.getMessage().contains(expectedKeyword));
        }
    }

    @Test
    public void testOperatorError() {
        analyzeExpectingError("def INT main() { INT x = 1 + 2.5; return 0; }", "OperatorError");
    }

    @Test
    public void testScopeError() {
        analyzeExpectingError("def INT main() { x = 10; return 0; }", "ScopeError");
    }

    @Test
    public void testMissingConditionError() {
        analyzeExpectingError("def INT main() { if (10) { return 1; } return 0; }", "MissingConditionError");
    }

    @Test
    public void testCollectionError() {
        analyzeExpectingError("coll P { INT x; } coll P { INT y; }", "CollectionError");
    }

    @Test
    public void testTypeError() {
        analyzeExpectingError("INT x = \"hello\";", "TypeError");
    }

    @Test
    public void testArgumentError() {
        String code = "def INT add(INT a) { return a; } def INT main() { return add(1, 2); }";
        analyzeExpectingError(code, "ArgumentError");
    }

    @Test
    public void testReturnError() {
        analyzeExpectingError("def INT f() { return true; }", "ReturnError");
    }
}