package compiler.Lexer;

public class Symbol {

    private final String value;

    public Symbol(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}