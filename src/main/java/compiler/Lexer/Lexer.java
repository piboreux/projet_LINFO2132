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

    public Symbol IdenOrKey() throws IOException{
        StringBuilder sb = new StringBuilder();
        char first = (char) currentChar;// UpperCase or Not
        // Letter, Number or _
        while(Character.isLetterOrDigit(currentChar)|| currentChar == '_'){
            sb.append((char)currentChar);
            advance();
        }
        String word = sb.toString();
        // keyword
        switch(word){
            case "final":  return new Symbol(word, Symbol.TokenType.FINAL);
            case "coll":   return new Symbol(word, Symbol.TokenType.COLL);
            case "def":    return new Symbol(word, Symbol.TokenType.DEF);
            case "for":    return new Symbol(word, Symbol.TokenType.FOR);
            case "while":  return new Symbol(word, Symbol.TokenType.WHILE);
            case "if":     return new Symbol(word, Symbol.TokenType.IF);
            case "else":   return new Symbol(word, Symbol.TokenType.ELSE);
            case "return": return new Symbol(word, Symbol.TokenType.RETURN);
            case "not":    return new Symbol(word, Symbol.TokenType.NOT);
            case "ARRAY":  return new Symbol(word, Symbol.TokenType.ARRAY);
            case "true":   return new Symbol(word, Symbol.TokenType.BOOLEAN);
            case "false":  return new Symbol(word, Symbol.TokenType.BOOLEAN);
        }
        if(Character.isUpperCase(first))return new Symbol(word,Symbol.TokenType.COLLECTION_NAME);
        return new Symbol(word, Symbol.TokenType.IDENTIFIER);
    }

    public Symbol Number() throws IOException{
        StringBuilder sb  = new StringBuilder();
        boolean IsFloat = false;
        if(currentChar == '.'){
            IsFloat = true;
            sb.append('0');// .23 e, 0.23
            sb.append('.');
            advance();
        }
        while(Character.isDigit(currentChar)){
            sb.append((char) currentChar);
            advance();

            if(currentChar == '.' && !IsFloat){
                IsFloat = true;
                sb.append('.');
                advance();
            }
        }
        String val = sb.toString();
        if (IsFloat){
            return new Symbol(val, Symbol.TokenType.FLOAT);
        }else{
            String newval = val.replaceFirst("^0+(?!$)", "");//remove 0(0024 = 24)
            return new Symbol(newval, Symbol.TokenType.INT);
        }
    }

    public Symbol String() throws IOException{
        StringBuilder sb = new StringBuilder();
        advance();// First "
        while(currentChar!= -1 && currentChar != '"'){
            if(currentChar == '\\'){
                advance();
                if(currentChar=='n'){
                    sb.append('\n');
                }else if (currentChar == '\\'){
                    sb.append('\\');
                }else if (currentChar == '"'){
                    sb.append('"');
                }else{
                    }
            }else{
                sb.append((char) currentChar);
            }
            advance();
        }
        if(currentChar==-1){
            throw new RuntimeException("Lexical Error: Unterminated string - missing last \" ");
        }
        advance();
        return new Symbol(sb.toString(), Symbol.TokenType.STRING);
    }

    public Symbol OperatorOrSymbol() throws IOException{
        char c = (char) currentChar;
        switch (c) {
            case '+':
                advance();
                return new Symbol("+", Symbol.TokenType.PLUS);
            case '-':
                advance();
                return new Symbol("-", Symbol.TokenType.MINUS);
            case '*':
                advance();
                return new Symbol("*", Symbol.TokenType.TIMES);
            case '/':
                advance();
                return new Symbol("/", Symbol.TokenType.DIV);
            case '%':
                advance();
                return new Symbol("%", Symbol.TokenType.MOD);
            case '(':
                advance();
                return new Symbol("(", Symbol.TokenType.LPAREN);
            case ')':
                advance();
                return new Symbol(")", Symbol.TokenType.RPAREN);
            case '{':
                advance();
                return new Symbol("{", Symbol.TokenType.LBRACE);
            case '}':
                advance();
                return new Symbol("}", Symbol.TokenType.RBRACE);
            case '[':
                advance();
                return new Symbol("[", Symbol.TokenType.LBRACKET);
            case ']':
                advance();
                return new Symbol("]", Symbol.TokenType.RBRACKET);
            case ';':
                advance();
                return new Symbol(";", Symbol.TokenType.SEMICOLON);
            case ',':
                advance();
                return new Symbol(",", Symbol.TokenType.COMMA);
            case '.':
                advance();
                return new Symbol(".", Symbol.TokenType.DOT);

            case '='://== or = or =/=
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Symbol("==", Symbol.TokenType.EQ);
                } else if (currentChar == '/') {
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Symbol("=/=", Symbol.TokenType.NEQ);
                    } else {
                        throw new RuntimeException(" Error: expected '=' after '=/'");
                    }
                }
                return new Symbol("=", Symbol.TokenType.ASSIGN);
            case '<'://< or <=
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Symbol("<=", Symbol.TokenType.LTE);
                }
                return new Symbol("<", Symbol.TokenType.LT);
            case '>': //> or >=
                advance();
                if (currentChar == '=') {
                    advance();
                    return new Symbol(">=", Symbol.TokenType.GTE);
                }
                return new Symbol(">", Symbol.TokenType.GT);
            case '&': //&& or Error
                advance();
                if (currentChar == '&') {
                    advance();
                    return new Symbol("&&", Symbol.TokenType.AND);
                }
                throw new RuntimeException("Error: expected '&' after '&'");
            case '|': // || or Error
                advance();
                if (currentChar == '|') {
                    advance();
                    return new Symbol("||", Symbol.TokenType.OR);
                }
                throw new RuntimeException("Error: expected '|' after '|'");
            default:
                throw new RuntimeException("Error: Unexpected character '" + c + "'");
        }
    }

    public Symbol getNextSymbol() {
        try {

            skipIgnored();
            //end of file
            if (currentChar == -1) {
                return new Symbol("", Symbol.TokenType.EOF);
            }
            // Identifiers or Keywords
            if (Character.isLetter(currentChar) || currentChar == '_') {
                return IdenOrKey();
            }
            //Int or Float
            if (Character.isDigit(currentChar) || currentChar == '.') {
                return Number();
            }
            //String
            if (currentChar == '"') {
                return String();
            }

            return OperatorOrSymbol();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}