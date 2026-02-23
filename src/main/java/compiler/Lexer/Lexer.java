package compiler.Lexer;

import java.io.IOException;
import java.io.Reader;

public class Lexer {

    private final Reader reader;
    private int currentChar;

    public Lexer(Reader input) {
        this.reader = input;
        try {
            advance(); // initialise le premier caractère
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void advance() throws IOException {
        currentChar = reader.read(); // -1 si fin de fichier
    }

    private void skipWhitespace() throws IOException {
        while (currentChar == ' ' ||
               currentChar == '\t' ||
               currentChar == '\n' ||
               currentChar == '\r') {
            advance();
        }
    }

    private void skipComment() throws IOException {
        if (currentChar == '#') {
            while (currentChar != '\n' && currentChar != -1) {
                advance();
            }
        }
    }

    private void skipIgnored() throws IOException {
        boolean skipping = true;

        while (skipping) {
            skipping = false;

            // ignorer espaces
            while (currentChar == ' ' ||
                   currentChar == '\t' ||
                   currentChar == '\n' ||
                   currentChar == '\r') {
                advance();
            }

            // ignorer commentaires
            if (currentChar == '#') {
                while (currentChar != '\n' && currentChar != -1) {
                    advance();
                }
                skipping = true; // re-vérifier whitespace après
            }
        }
    }

    public Symbol getNextSymbol() {
        try {

            skipIgnored();

            if (currentChar == -1) {
                return new Symbol("EOF");
            }

            char c = (char) currentChar;
            advance();

            return new Symbol(String.valueOf(c));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}