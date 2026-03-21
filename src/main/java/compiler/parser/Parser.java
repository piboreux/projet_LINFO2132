package compiler.parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.parser.AST.*;
import compiler.parser.AST.literals.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Lexer lexer;
    private Symbol currentSymbol;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentSymbol = lexer.getNextSymbol();
    }

    // Base function

    //get next token
    private void consume(Symbol.TokenType expected) {
        if (currentSymbol.getType() == expected) {
            Symbol s = currentSymbol;
            currentSymbol = lexer.getNextSymbol();
            return;
        }
        throw new RuntimeException("Syntax Error: Expected " + expected
                + " but found " + currentSymbol.getType()
                + " (\"" + currentSymbol.getValue() + "\")");
    }
    // just to get the type of the symbol
    private Symbol.TokenType peek() {
        return currentSymbol.getType();
    }

    private boolean check(Symbol.TokenType t) {
        return currentSymbol.getType() == t;
    }

    // method to know if we have int, float, string, bool, array or collection
    private boolean isTypeToken() {
        switch (peek()) {
            case INT: case FLOAT: case STRING: case BOOL: case ARRAY:
            case COLLECTION_NAME:
                return true;
            default:
                return false;
        }
    }

    // FIRST FUNCTION
    public ASTNode getAST() {
        return parseProgram();
    }

    private ASTNode parseProgram() {
        List<ASTNode> instr = new ArrayList<>();
        while (!check(Symbol.TokenType.EOF)) {
            instr.add(parseStatement());
        }
        consume(Symbol.TokenType.EOF);
        return new ProgramNode(instr);
    }


    //statement
    private ASTNode parseStatement() {
        switch (peek()) {
            case FINAL:
                consume(Symbol.TokenType.FINAL);
                return parseVarDeclOrArrayDecl(true);
            case INT: case FLOAT: case STRING:
            case BOOL:
                return parseVarDeclOrArrayDecl(false);
            case COLLECTION_NAME:
                return parseVarDeclCollection();
            case DEF:
                return parseFuncDecl();
            case COLL:
                return parseCollDecl();
            case IF:
                return parseIf();
            case WHILE:
                return parseWhile();
            case FOR:
                return parseFor();
            case RETURN:
                return parseReturn();
            case LBRACE:
                return parseBlock();
            case IDENTIFIER:
                return parseIdentifierStatement();
            default:
                throw new RuntimeException("Syntax Error: Unexpected token "
                        + peek() + " (\"" + currentSymbol.getValue() + "\")");
        }
    }

    // (final) INT id = ...  or  (final) INT[] id = ... or int id;
    private ASTNode parseVarDeclOrArrayDecl(boolean isFinal) {
        String typeName = currentSymbol.getValue();
        consume(peek());

        // []
        if (check(Symbol.TokenType.LBRACKET)) {
            consume(Symbol.TokenType.LBRACKET);
            consume(Symbol.TokenType.RBRACKET);
            typeName = typeName + "[]";
        }

        String varName = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);

        // int a ;
        if (check(Symbol.TokenType.SEMICOLON)) {
            consume(Symbol.TokenType.SEMICOLON);
            return new VarDeclNode(typeName, varName, isFinal, null);
        }

        consume(Symbol.TokenType.ASSIGN);
        ASTNode expr = parseExpression();
        consume(Symbol.TokenType.SEMICOLON);
        return new VarDeclNode(typeName, varName, isFinal, expr);
    }

    // CollName id = ... ;
    private ASTNode parseVarDeclCollection() {
        String typeName = currentSymbol.getValue();
        consume(Symbol.TokenType.COLLECTION_NAME);

        // []
        if (check(Symbol.TokenType.LBRACKET)) {
            consume(Symbol.TokenType.LBRACKET);
            consume(Symbol.TokenType.RBRACKET);
            typeName = typeName + "[]";
        }

        String varName = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);

        //CollName a ;
        if (check(Symbol.TokenType.SEMICOLON)) {
            consume(Symbol.TokenType.SEMICOLON);
            return new VarDeclNode(typeName, varName, false, null);
        }

        consume(Symbol.TokenType.ASSIGN);
        ASTNode expr = parseExpression();
        consume(Symbol.TokenType.SEMICOLON);
        return new VarDeclNode(typeName, varName, false, expr);
    }

    // identifier(x=5, a(),...)
    private ASTNode parseIdentifierStatement() {
        String name = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);

        // function call a(...)
        if (check(Symbol.TokenType.LPAREN)) {
            consume(Symbol.TokenType.LPAREN);
            List<ASTNode> args = parseArgList();
            consume(Symbol.TokenType.RPAREN);
            consume(Symbol.TokenType.SEMICOLON);
            return new CallNode(name, args);
        }

        // a[i], a.i
        ASTNode target = new IdentifierNode(name);
        while (check(Symbol.TokenType.LBRACKET) || check(Symbol.TokenType.DOT)) { //a[1].i
            if (check(Symbol.TokenType.LBRACKET)) {
                consume(Symbol.TokenType.LBRACKET);
                ASTNode idx = parseExpression();
                consume(Symbol.TokenType.RBRACKET);
                target = new ArrayAccessNode(target, idx);
            } else {
                consume(Symbol.TokenType.DOT);
                String field = currentSymbol.getValue();
                consume(Symbol.TokenType.IDENTIFIER);
                target = new FieldAccessNode(target, field);
            }
        }

        // a = 2
        consume(Symbol.TokenType.ASSIGN);
        ASTNode value = parseExpression();
        consume(Symbol.TokenType.SEMICOLON);
        return new AssignNode(target, value);
    }

    // def
    private ASTNode parseFuncDecl() {
        consume(Symbol.TokenType.DEF);

        // def INT a() -> INT
        String returnType = "void";
        if (isTypeToken()) {
            if (check(Symbol.TokenType.COLLECTION_NAME)) {
                returnType = currentSymbol.getValue();
                consume(Symbol.TokenType.COLLECTION_NAME);
            } else {
                returnType = currentSymbol.getValue();
                consume(peek());
            }
            // return array
            if (check(Symbol.TokenType.LBRACKET)) {
                consume(Symbol.TokenType.LBRACKET);
                consume(Symbol.TokenType.RBRACKET);
                returnType = returnType + "[]";
            }
        }

        String name = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);
        consume(Symbol.TokenType.LPAREN);
        List<VarDeclNode> params = parseParamList();
        consume(Symbol.TokenType.RPAREN);
        ASTNode body = parseBlock();
        return new FuncDeclNode(name, returnType, params, body);
    }

    // coll A{...}
    private ASTNode parseCollDecl() {
        consume(Symbol.TokenType.COLL);
        String name = currentSymbol.getValue();
        consume(Symbol.TokenType.COLLECTION_NAME);
        consume(Symbol.TokenType.LBRACE);
        List<VarDeclNode> fields = new ArrayList<>();
        while (!check(Symbol.TokenType.RBRACE) && !check(Symbol.TokenType.EOF)) {
            String typeName = currentSymbol.getValue();
            consume(peek());

            // array
            if (check(Symbol.TokenType.LBRACKET)) {
                consume(Symbol.TokenType.LBRACKET);
                consume(Symbol.TokenType.RBRACKET);
                typeName = typeName + "[]";
            }

            String fieldName = currentSymbol.getValue();
            consume(Symbol.TokenType.IDENTIFIER);
            consume(Symbol.TokenType.SEMICOLON);
            fields.add(new VarDeclNode(typeName, fieldName, false, null));
        }
        consume(Symbol.TokenType.RBRACE);
        return new CollDeclNode(name, fields);
    }

    // if ( expr ) block [ else (if | block) ]
    private ASTNode parseIf() {
        consume(Symbol.TokenType.IF);
        consume(Symbol.TokenType.LPAREN);
        ASTNode cond = parseExpression();
        consume(Symbol.TokenType.RPAREN);
        ASTNode thenBlock = parseBlock();
        ASTNode elseBlock = null;
        if (check(Symbol.TokenType.ELSE)) {
            consume(Symbol.TokenType.ELSE);
            elseBlock = check(Symbol.TokenType.IF) ? parseIf() : parseBlock();
        }
        return new IfNode(cond, thenBlock, elseBlock);
    }

    // while
    private ASTNode parseWhile() {
        consume(Symbol.TokenType.WHILE);
        consume(Symbol.TokenType.LPAREN);
        ASTNode cond = parseExpression();
        consume(Symbol.TokenType.RPAREN);
        return new WhileNode(cond, parseBlock());
    }

    // for ( TYPE id ; start -> end ; increment ) block
    private ASTNode parseFor() {
        consume(Symbol.TokenType.FOR);
        consume(Symbol.TokenType.LPAREN);

        String typeName = currentSymbol.getValue();
        consume(peek());
        String varName = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);
        consume(Symbol.TokenType.SEMICOLON);

        // Range
        ASTNode start = parseExpression();
        consume(Symbol.TokenType.RANGE); // ->
        ASTNode end = parseExpression();
        consume(Symbol.TokenType.SEMICOLON);

        // Incrément
        ASTNode increment = parseExpression();
        consume(Symbol.TokenType.RPAREN);

        VarDeclNode iterator = new VarDeclNode(typeName, varName, false, null);
        return new ForNode(iterator, start, end, increment, parseBlock());
    }

    // return
    private ASTNode parseReturn() {
        consume(Symbol.TokenType.RETURN);
        ASTNode expr = null;
        if (!check(Symbol.TokenType.SEMICOLON)) {
            expr = parseExpression();
        }
        consume(Symbol.TokenType.SEMICOLON);
        return new ReturnNode(expr);
    }

    // body funct, if/else, while,for
    private ASTNode parseBlock() {
        consume(Symbol.TokenType.LBRACE);
        List<ASTNode> instr = new ArrayList<>();
        while (!check(Symbol.TokenType.RBRACE) && !check(Symbol.TokenType.EOF)) {
            instr.add(parseStatement());
        }
        consume(Symbol.TokenType.RBRACE);
        return new BlockNode(instr);
    }

    // expressions avec ordre

    private ASTNode parseExpression() {
        return parseOr();
    }

    // or → and
    private ASTNode parseOr() {
        ASTNode node = parseAnd();
        while (check(Symbol.TokenType.OR)) {
            consume(Symbol.TokenType.OR);
            node = new BinaryOpNode("||", node, parseAnd());
        }
        return node;
    }

    // and → (in)-equality
    private ASTNode parseAnd() {
        ASTNode node = parseEquality();
        while (check(Symbol.TokenType.AND)) {
            consume(Symbol.TokenType.AND);
            node = new BinaryOpNode("&&", node, parseEquality());
        }
        return node;
    }

    // equality → comparison
    private ASTNode parseEquality() {
        ASTNode node = parseComparison();
        while (check(Symbol.TokenType.EQ) || check(Symbol.TokenType.NEQ)) {
            String op = currentSymbol.getValue();
            consume(peek());
            node = new BinaryOpNode(op, node, parseComparison());
        }
        return node;
    }

    // comparison → addition
    private ASTNode parseComparison() {
        ASTNode node = parseAddition();
        while (check(Symbol.TokenType.LT) || check(Symbol.TokenType.GT)
                || check(Symbol.TokenType.LTE) || check(Symbol.TokenType.GTE)) {
            String op = currentSymbol.getValue();
            consume(peek());
            node = new BinaryOpNode(op, node, parseAddition());
        }
        return node;
    }

    // addition → multiplication
    private ASTNode parseAddition() {
        ASTNode node = parseMultiplication();
        while (check(Symbol.TokenType.PLUS) || check(Symbol.TokenType.MINUS)) {
            String op = currentSymbol.getValue();
            consume(peek());
            node = new BinaryOpNode(op, node, parseMultiplication());
        }
        return node;
    }

    // multiplication → unary
    private ASTNode parseMultiplication() {
        ASTNode node = parseUnary();
        while (check(Symbol.TokenType.TIMES) || check(Symbol.TokenType.DIV)
                || check(Symbol.TokenType.MOD)) {
            String op = currentSymbol.getValue();
            consume(peek());
            node = new BinaryOpNode(op, node, parseUnary());
        }
        return node;
    }

    // unary → not,-
    private ASTNode parseUnary() {
        if (check(Symbol.TokenType.NOT)) {
            consume(Symbol.TokenType.NOT);
            return new UnaryOpNode("not", parseUnary());
        }
        if (check(Symbol.TokenType.MINUS)) {
            consume(Symbol.TokenType.MINUS);
            return new UnaryOpNode("-", parseUnary());
        }
        return parsePostfix();
    }

    // postfix → primary
    private ASTNode parsePostfix() {
        ASTNode node = parsePrimary();
        while (check(Symbol.TokenType.DOT) || check(Symbol.TokenType.LBRACKET)) {
            if (check(Symbol.TokenType.DOT)) {
                consume(Symbol.TokenType.DOT);
                String field = currentSymbol.getValue();
                consume(Symbol.TokenType.IDENTIFIER);
                if (check(Symbol.TokenType.LPAREN)) {
                    consume(Symbol.TokenType.LPAREN);
                    List<ASTNode> args = parseArgList();
                    consume(Symbol.TokenType.RPAREN);
                    node = new MethodCallNode(node, field, args);
                } else {
                    node = new FieldAccessNode(node, field);
                }
            } else {
                consume(Symbol.TokenType.LBRACKET);
                ASTNode idx = parseExpression();
                consume(Symbol.TokenType.RBRACKET);
                node = new ArrayAccessNode(node, idx);
            }
        }
        return node;
    }

    // primary
    private ASTNode parsePrimary() {
        switch (peek()) {
            case INT: {
                String val = currentSymbol.getValue();
                consume(Symbol.TokenType.INT);
                // array
                if (check(Symbol.TokenType.ARRAY)) {
                    consume(Symbol.TokenType.ARRAY);
                    consume(Symbol.TokenType.LBRACKET);
                    ASTNode size = parseExpression();
                    consume(Symbol.TokenType.RBRACKET);
                    return new ArrayConstructorNode("INT", size);
                }
                return new IntLiteralNode(val);
            }
            case FLOAT: {
                String val = currentSymbol.getValue();
                consume(Symbol.TokenType.FLOAT);
                if (check(Symbol.TokenType.ARRAY)) {
                    consume(Symbol.TokenType.ARRAY);
                    consume(Symbol.TokenType.LBRACKET);
                    ASTNode size = parseExpression();
                    consume(Symbol.TokenType.RBRACKET);
                    return new ArrayConstructorNode("FLOAT", size);
                }
                return new FloatLiteralNode(val);
            }
            case STRING: {
                String val = currentSymbol.getValue();
                consume(Symbol.TokenType.STRING);
                if (check(Symbol.TokenType.ARRAY)) {
                    consume(Symbol.TokenType.ARRAY);
                    consume(Symbol.TokenType.LBRACKET);
                    ASTNode size = parseExpression();
                    consume(Symbol.TokenType.RBRACKET);
                    return new ArrayConstructorNode("STRING", size);
                }
                return new StringLiteralNode(val);
            }
            case BOOL: {
                String val = currentSymbol.getValue();
                consume(Symbol.TokenType.BOOL);
                if (check(Symbol.TokenType.ARRAY)) {
                    consume(Symbol.TokenType.ARRAY);
                    consume(Symbol.TokenType.LBRACKET);
                    ASTNode size = parseExpression();
                    consume(Symbol.TokenType.RBRACKET);
                    return new ArrayConstructorNode("BOOL", size);
                }
                return new BoolLiteralNode(val);
            }
            case BOOLEAN_LITERAL: {
                String val = currentSymbol.getValue();
                consume(Symbol.TokenType.BOOLEAN_LITERAL);
                return new BoolLiteralNode(val);
            }
            case LPAREN: {//(1+2)*3
                consume(Symbol.TokenType.LPAREN);
                ASTNode expr = parseExpression();
                consume(Symbol.TokenType.RPAREN);
                return expr;
            }
            case IDENTIFIER: {
                String name = currentSymbol.getValue();
                consume(Symbol.TokenType.IDENTIFIER);
                if (check(Symbol.TokenType.LPAREN)) {
                    consume(Symbol.TokenType.LPAREN);
                    List<ASTNode> args = parseArgList();
                    consume(Symbol.TokenType.RPAREN);
                    return new CallNode(name, args);
                }
                return new IdentifierNode(name);
            }
            case COLLECTION_NAME: {
                String name = currentSymbol.getValue();
                consume(Symbol.TokenType.COLLECTION_NAME);
                consume(Symbol.TokenType.LPAREN);
                List<ASTNode> args = parseArgList();
                consume(Symbol.TokenType.RPAREN);
                return new CallNode(name, args);
            }
            default:
                throw new RuntimeException("Syntax Error: Unexpected token in expression: "
                        + peek() + " (\"" + currentSymbol.getValue() + "\")");
        }
    }

    //helper List

    // arg List (call function)
    private List<ASTNode> parseArgList() {
        List<ASTNode> args = new ArrayList<>();
        if (!check(Symbol.TokenType.RPAREN)) {
            args.add(parseExpression());
            while (check(Symbol.TokenType.COMMA)) {
                consume(Symbol.TokenType.COMMA);
                args.add(parseExpression());
            }
        }
        return args;
    }

    // param List(decl function)
    private List<VarDeclNode> parseParamList() {
        List<VarDeclNode> params = new ArrayList<>();
        if (!check(Symbol.TokenType.RPAREN)) {
            params.add(parseParam());
            while (check(Symbol.TokenType.COMMA)) {
                consume(Symbol.TokenType.COMMA);
                params.add(parseParam());
            }
        }
        return params;
    }

    private VarDeclNode parseParam() {
        String typeName;
        if (check(Symbol.TokenType.COLLECTION_NAME)) {
            typeName = currentSymbol.getValue();
            consume(Symbol.TokenType.COLLECTION_NAME);
        } else {
            typeName = currentSymbol.getValue();
            consume(peek());
        }
        // array
        if (check(Symbol.TokenType.LBRACKET)) {
            consume(Symbol.TokenType.LBRACKET);
            consume(Symbol.TokenType.RBRACKET);
            typeName = typeName + "[]";
        }
        String paramName = currentSymbol.getValue();
        consume(Symbol.TokenType.IDENTIFIER);
        return new VarDeclNode(typeName, paramName, false, null);
    }
}