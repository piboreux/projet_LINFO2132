package compiler.Lexer;

public class Symbol {

    //types de tokens
    public enum TokenType {
        //IDENTIFIER (= names of variables, functions, types)
        IDENTIFIER,
        COLLECTION_NAME,//begin with Capital letter

        //keywords
        FINAL, COLL, DEF, FOR, WHILE, IF, ELSE, RETURN, NOT, ARRAY,
        //Base Type
        INT,FLOAT,STRING,BOOLEAN,

        //OPERATOR
        PLUS, MINUS, TIMES, DIV, MOD,// +, -, *, /, %
        ASSIGN,        // =
        EQ, NEQ,       // ==, =/=
        LT, GT, LTE, GTE, // <, >, <=, >=
        AND, OR, // &&, ||

        //rest of symbol
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, // (, ), {, }, [,]
        SEMICOLON, COMMA, DOT, // ;, ., ','

        //End of File
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
        return "<" + type + ", " + value + ">";
    }
}