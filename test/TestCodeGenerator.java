import static org.junit.Assert.*;

import org.junit.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import compiler.Lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.AST.ASTNode;
import compiler.parser.AST.ProgramNode;
import compiler.Semantic_Analysis.Semantic_Analysis;
import compiler.codegen.CodeGenerator;

public class TestCodeGenerator {

    private String compileAndRun(String code) throws Exception {
        Lexer lexer = new Lexer(new StringReader(code));
        Parser parser = new Parser(lexer);
        ASTNode ast = parser.getAST();

        assertTrue(ast instanceof ProgramNode);

        ProgramNode program = (ProgramNode) ast;

        Semantic_Analysis analyzer = new Semantic_Analysis();
        analyzer.analyze(program);

        Path outputDir = Files.createTempDirectory("codegen-test-");

        CodeGenerator generator = new CodeGenerator("test", outputDir.toString());
        generator.generate(program);

        Process process = new ProcessBuilder("java", "-cp", outputDir.toString(), "test")
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        assertEquals(0, exitCode);
        return output.trim();
    }

    @Test
    public void testHello() throws Exception {
        String code = "def main() { println(\"hello\"); }";
        assertEquals("hello", compileAndRun(code));
    }

    @Test
    public void testFunctionReturn() throws Exception {
        String code =
                "def INT add(INT a, INT b) { return a + b; }" +
                "def main() { println(add(20, 22)); }";

        assertEquals("42", compileAndRun(code));
    }

    @Test
    public void testVoidFunction() throws Exception {
        String code =
                "def say() { println(\"ok\"); }" +
                "def main() { say(); }";

        assertEquals("ok", compileAndRun(code));
    }

    @Test
    public void testScopes() throws Exception {
        String code =
                "def f() { INT x = 1; println(x); }" +
                "def main() { INT x = 2; println(x); f(); }";

        assertEquals("2\n1", compileAndRun(code));
    }

    @Test
    public void testFloatAndBool() throws Exception {
        String code =
                "def FLOAT add(FLOAT a, FLOAT b) { return a + b; }" +
                "def main() {" +
                " println(add(1.5, 2.5));" +
                " if (2.0 < 3.0 && true) { println(\"ok\"); } else { println(\"bad\"); }" +
                "}";

        assertEquals("4.0\nok", compileAndRun(code));
    }

    @Test
    public void testArrays() throws Exception {
        String code =
                "def main() {" +
                " INT[] ti = INT ARRAY [3]; ti[0] = 42;" +
                " FLOAT[] tf = FLOAT ARRAY [2]; tf[0] = 3.5;" +
                " BOOL[] tb = BOOL ARRAY [2]; tb[0] = true;" +
                " println(ti[0]); println(tf[0]); println(tb[0]);" +
                "}";

        assertEquals("42\n3.5\ntrue", compileAndRun(code));
    }

    @Test
    public void testCollections() throws Exception {
        String code =
                "coll Point { INT x; INT y; }" +
                "def printPoint(Point p) { println(p.x); println(p.y); }" +
                "def main() { Point p = Point(1, 2); printPoint(p); }";

        assertEquals("1\n2", compileAndRun(code));
    }

    @Test
    public void testIntToFloatPromotionInReturn() throws Exception {
        String code =
                "def FLOAT f() { return 2; }" +
                "def main() { println(f()); }";

        assertEquals("2.0", compileAndRun(code));
    }

    @Test
    public void testBuiltins() throws Exception {
        String code =
                "def main() {" +
                " println(str(42));" +
                " println(floor(3.8));" +
                " println(ceil(3.2));" +
                " println(length(\"hello\"));" +
                "}";

        assertEquals("42\n3\n4\n5", compileAndRun(code));
    }
}
