package compiler.Lexer;

public class Symbol {

    //types de tokens
    public enum TokenType {
        // types
        INT, FLOAT, STRING, BOOL,      
        // littéraux
        BOOLEAN_LITERAL,              
        // mots-clés
        FINAL, COLL, DEF, FOR, WHILE, IF, ELSE, RETURN, NOT, ARRAY,
        // identifiants
        IDENTIFIER, COLLECTION_NAME,
        // opérateurs
        PLUS, MINUS, TIMES, DIV, MOD, ASSIGN, EQ, NEQ,
        LT, LTE, GT, GTE, AND, OR, RANGE,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
        SEMICOLON, COMMA, DOT,
        EOF
    }
    private final String value;
    private final TokenType type;

    public Symbol(String value, TokenType type) {
        this.value = value;
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }
    public String getValue() {
        return value;
    }

    public String toString() {
        if (value == null || value.isEmpty()) {
            return "<" + type + ">";
        }
        return "<" + type + ", \"" + value + "\">";
    }
}