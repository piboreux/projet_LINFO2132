import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.io.StringReader;
import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;

public class TestLexer {

    @Test
    public void testSequence() {
        // "def" est un mot-clé, "x" un identifiant, "=" un assign, "2" un int
        String input = "def x = 2;";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);

        // 1. Vérifier "def"
        Symbol s1 = lexer.getNextSymbol();
        System.out.println(s1.toString());
        assertEquals(Symbol.TokenType.DEF, s1.getType());
        assertEquals("def", s1.getValue());

        // 2. Vérifier "x"
        Symbol s2 = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.IDENTIFIER, s2.getType());
        assertEquals("x", s2.getValue());

        // 3. Vérifier "="
        Symbol s3 = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.ASSIGN, s3.getType());

        // 4. Vérifier "2"
        Symbol s4 = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.INT, s4.getType());
        assertEquals("2", s4.getValue());

        // 5. Vérifier ";"
        Symbol s5 = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.SEMICOLON, s5.getType());

        // 6. Vérifier la fin de fichier
        Symbol s6 = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.EOF, s6.getType());
    }
}