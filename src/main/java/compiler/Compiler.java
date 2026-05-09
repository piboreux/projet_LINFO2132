package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.parser.AST.ASTNode;
import compiler.parser.AST.ProgramNode;
import compiler.parser.Parser;
import compiler.Semantic_Analysis.Semantic_Analysis;
import compiler.codegen.CodeGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Compiler {

    public static void main(String[] args) {

        if (args.length >= 2 && args[0].equals("-lexer")) {

            try {
                FileReader reader = new FileReader(args[1]);
                Lexer lexer = new Lexer(reader);

                Symbol symbol;

                while (true) {
                    symbol = lexer.getNextSymbol();
                    System.out.println(symbol);
                    /*
                    if (symbol.toString().equals("EOF")) {
                        break;
                    }
                    */
                    if (symbol.getType() == Symbol.TokenType.EOF){
                        break;
                    }
                }

            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }


        }else if (args.length >= 2 && args[0].equals("-parser")) {
            try {
                FileReader reader = new FileReader(args[1]);
                Lexer lexer = new Lexer(reader);
                Parser parser = new Parser(lexer);

                ASTNode root = parser.getAST();
                System.out.print(root.toString());

            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        } else if (args.length >= 2 && args[0].equals("-semantic")) {
            try {
                FileReader reader = new FileReader(args[1]);
                Lexer lexer = new Lexer(reader);
                Parser parser = new Parser(lexer);
                ASTNode root = parser.getAST();

                if (root instanceof ProgramNode program) {
                    Semantic_Analysis analyzer = new Semantic_Analysis();
                    analyzer.analyze(program);
                    System.out.println("OK");
                    System.exit(0);
                } else {
                    System.err.println("Error: Root is not a ProgramNode");
                    System.exit(1);
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(1);
            } catch (Semantic_Analysis.SemanticException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }

        } else if (args.length >= 1 && !args[0].startsWith("-")) {
            String sourceFile = args[0];
            String targetPath = deriveDefaultTarget(sourceFile);
            if (args.length >= 3 && args[1].equals("-o")) {
                targetPath = args[2];
            }
            File   targetFile   = new File(targetPath);
            String outputDir    = targetFile.getParent() != null
                    ? targetFile.getParent()
                    : ".";
            String rawName      = targetFile.getName();
            String mainClassName = rawName.endsWith(".class")
                    ? rawName.substring(0, rawName.length() - 6)
                    : rawName;
            try {
                Lexer  lexer  = new Lexer(new FileReader(sourceFile));
                Parser parser = new Parser(lexer);
                ASTNode root  = parser.getAST();
                if (!(root instanceof ProgramNode program)) {
                    System.err.println("Error: Root is not a ProgramNode");
                    System.exit(1);
                    return;
                }
                new Semantic_Analysis().analyze(program);
                new CodeGenerator(mainClassName, outputDir).generate(program);
                System.exit(0);

            } catch (Semantic_Analysis.SemanticException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(1);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println(
                    "Usage:\n" + "  -lexer   <source_file>\n" + "  -parser  <source_file>\n" + "  -semantic <source_file>\n" + "  <source_file> [-o <target_file>]"
            );
            System.exit(1);
        }
    }
    private static String deriveDefaultTarget(String sourceFile) {
        File src = new File(sourceFile);
        String dir = src.getParent() != null ? src.getParent() : ".";
        return dir + File.separator + "Main.class";
    }
}