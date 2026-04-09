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

    private void analyzeExpectingSuccess(String input) {
        try {
            ASTNode ast = parse(input);
            Semantic_Analysis analyzer = new Semantic_Analysis();
            if (ast instanceof ProgramNode program) {
                analyzer.analyze(program);
            }
        } catch (Exception e) {
            fail("Expected no error but got: " + e.getMessage());
        }
    }

    private void analyzeExpectingError(String input, String expectedErrorKeyword) {
        try {
            ASTNode ast = parse(input);
            Semantic_Analysis analyzer = new Semantic_Analysis();
            if (ast instanceof ProgramNode program) {
                analyzer.analyze(program);
            }
            fail("Expected error with keyword '" + expectedErrorKeyword + "' but none was thrown");
        } catch (RuntimeException e) {
            assertTrue("Expected error keyword '" + expectedErrorKeyword + "' in: " + e.getMessage(),
                    e.getMessage().contains(expectedErrorKeyword));
        }
    }


    @Test
    public void testBasicValidDeclaration() {
        analyzeExpectingSuccess("INT x = 5;");
    }

    @Test
    public void testBasicTypeError() {
        analyzeExpectingError("INT x = 3.14;", "TypeError");
    }
    @Test
    public void testValidFloatVarDeclaration() {
        analyzeExpectingSuccess("FLOAT f = 3.14;");
    }

}

