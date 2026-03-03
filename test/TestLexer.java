import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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


    @Test
    public void testFloatLeadingDot() {
        Lexer lexer = new Lexer(new StringReader(".234"));
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.FLOAT, s.getType());
        assertEquals("0.234", s.getValue());
    }

    @Test
    public void testLeadingZeros() {
        Lexer lexer = new Lexer(new StringReader("00342"));
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.INT, s.getType());
        assertEquals("342", s.getValue());
    }

    @Test
    public void testUnterminatedString() {
        assertThrows(RuntimeException.class, () -> {
            Lexer lexer = new Lexer(new StringReader("\"Hello"));
            lexer.getNextSymbol();
        });
    }

    @Test
    public void testInvalidCharacter() {
        assertThrows(RuntimeException.class, () -> {
            Lexer lexer = new Lexer(new StringReader("@"));
            lexer.getNextSymbol();
        });
    }

    @Test
    public void testRangeOperator() {
        Lexer lexer = new Lexer(new StringReader("1 -> 10"));
        lexer.getNextSymbol(); // 1
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.RANGE, s.getType());
        assertEquals("->", s.getValue());
    }

    @Test
    public void testComment() {
        Lexer lexer = new Lexer(new StringReader("# ceci est un commentaire\n42"));
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.INT, s.getType());
        assertEquals("42", s.getValue());
    }

    @Test
    public void testBooleanLiterals() {
        Lexer lexer = new Lexer(new StringReader("true false"));
        assertEquals(Symbol.TokenType.BOOLEAN_LITERAL, lexer.getNextSymbol().getType());
        assertEquals(Symbol.TokenType.BOOLEAN_LITERAL, lexer.getNextSymbol().getType());
    }

    @Test
    public void testCollectionName() {
        Lexer lexer = new Lexer(new StringReader("MyCollection"));
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.COLLECTION_NAME, s.getType());
        assertEquals("MyCollection", s.getValue());
    }

    @Test
    public void testStringEscapes() {
        Lexer lexer = new Lexer(new StringReader("\"hello\\nworld\""));
        Symbol s = lexer.getNextSymbol();
        assertEquals(Symbol.TokenType.STRING, s.getType());
        assertEquals("hello\nworld", s.getValue());
    }
}