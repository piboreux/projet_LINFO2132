package compiler.Semantic_Analysis;
import compiler.parser.AST.*;
import compiler.parser.AST.literals.*;
import java.util.*;
public class Semantic_Analysis {

    private static class SymbolInfo {
        String type;
        boolean isFinal;
        boolean isFunction;
        List<String> paramTypes;
        String returnType;
        SymbolInfo(String type, boolean isFinal) {
            this.type       = type;
            this.isFinal    = isFinal;
            this.isFunction = false;
        }
        SymbolInfo(String returnType, List<String> paramTypes) {
            this.type       = returnType;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.isFunction = true;
            this.isFinal    = false;
        }
    }
    private static class SymbolTable {
        private final Deque<Map<String, SymbolInfo>> scopes = new ArrayDeque<>();
        private static final Set<String> PRIMITIVE_TYPES = Set.of("INT", "FLOAT", "BOOL", "STRING", "ARRAY");
        private final Map<String, LinkedHashMap<String, String>> collectionFieldTypes = new LinkedHashMap<>();
        void enterScope() { scopes.push(new LinkedHashMap<>()); }
        void exitScope()  { scopes.pop(); }
        void declare(String name, SymbolInfo info) {
            if (scopes.isEmpty()) semanticError("ScopeError", "No active scope to declare '" + name + "'");
            if (scopes.peek().containsKey(name))  semanticError("ScopeError", "'" + name + "' is already declared in this scope"); //can't have 2* same name
            scopes.peek().put(name, info);
        }
        SymbolInfo lookup(String name) {
            for (var scope : scopes)
                if (scope.containsKey(name)) return scope.get(name);
            semanticError("ScopeError", "'" + name + "' is used but not declared");
            return null;
        }
        void registerCollection(String name, List<VarDeclNode> fields) {
            if (!Character.isUpperCase(name.charAt(0))) semanticError("CollectionError", "Collection name '" + name + "' must start with a capital letter");
            if (PRIMITIVE_TYPES.contains(name)) semanticError("CollectionError", "'" + name + "' is a built-in type and cannot be redefined as a collection");
            if (collectionFieldTypes.containsKey(name)) semanticError("CollectionError", "Collection '" + name + "' is already defined");
            LinkedHashMap<String, String> fieldMap = new LinkedHashMap<>();
            for (VarDeclNode f : fields)
                fieldMap.put(f.getName(), f.getType());
            collectionFieldTypes.put(name, fieldMap);
        }
        boolean isCollectionType(String name) {
            return collectionFieldTypes.containsKey(name);
        }
        boolean isValidType(String type) {
            String base = type.endsWith("[]") ? type.substring(0, type.length() - 2) : type;
            return PRIMITIVE_TYPES.contains(base) || collectionFieldTypes.containsKey(base);
        }
        List<String> getCollectionFieldTypes(String collName) {
            LinkedHashMap<String, String> fields = collectionFieldTypes.get(collName);
            if (fields == null) return Collections.emptyList();
            return new ArrayList<>(fields.values());
        }
        String getFieldType(String collName, String fieldName) {
            LinkedHashMap<String, String> fields = collectionFieldTypes.get(collName);
            if (fields == null || !fields.containsKey(fieldName))semanticError("TypeError", "Collection '" + collName + "' has no field '" + fieldName + "'");
            return fields.get(fieldName);
        }
    }
    private final SymbolTable symbolTable = new SymbolTable();
    private String currentFunctionReturnType = null;

    public void analyze(ProgramNode program) {
        symbolTable.enterScope();
        registerBuiltInFunctions();
        for (ASTNode node : program.getChildren()) {
            if (node instanceof CollDeclNode coll) symbolTable.registerCollection(coll.getName(), coll.getFields());
            else if (node instanceof FuncDeclNode fn) registerFunction(fn);
        }
        for (ASTNode node : program.getChildren()) {
            if (node instanceof FuncDeclNode fn) {
                analyzeFunction(fn);
            } else if (node instanceof VarDeclNode var) {
                analyzeVarDeclaration(var);
            } else {
                analyzeStatement(node);
            }
        }
        symbolTable.exitScope();
    }
    private void registerFunction(FuncDeclNode node) {
        List<String> paramTypes = new ArrayList<>();
        for (VarDeclNode p : node.getParameters())
            paramTypes.add(p.getType());
        symbolTable.declare(node.getName(), new SymbolInfo(node.getReturnType(), paramTypes));
    }
    private void registerBuiltInFunctions() {
        // Manipulation functions
        symbolTable.declare("not", new SymbolInfo("BOOL", List.of("BOOL")));
        symbolTable.declare("str", new SymbolInfo("STRING", List.of("INT")));
        symbolTable.declare("floor", new SymbolInfo("INT", List.of("FLOAT")));
        symbolTable.declare("ceil", new SymbolInfo("INT", List.of("FLOAT")));

        // I/O functions (read functions)
        symbolTable.declare("read_INT", new SymbolInfo("INT", List.of()));
        symbolTable.declare("read_FLOAT", new SymbolInfo("FLOAT", List.of()));
        symbolTable.declare("read_STRING", new SymbolInfo("STRING", List.of()));

        // I/O functions (print functions) - return void
        symbolTable.declare("print_INT", new SymbolInfo("void", List.of("INT")));
        symbolTable.declare("print_FLOAT", new SymbolInfo("void", List.of("FLOAT")));

        // Special functions that accept any type
        symbolTable.declare("print", new SymbolInfo("void", List.of("ANY")));
        symbolTable.declare("println", new SymbolInfo("void", List.of("ANY")));

        // Special function: length (handles both STRING and ARRAY)
        symbolTable.declare("length", new SymbolInfo("INT", List.of("ANY")));
    }
    private void analyzeFunction(FuncDeclNode node) {
        String previousReturnType = currentFunctionReturnType;
        currentFunctionReturnType = node.getReturnType();
        symbolTable.enterScope();
        for (VarDeclNode p : node.getParameters()) {
            if (!symbolTable.isValidType(p.getType())) {
                semanticError("TypeError", "Unknown type '" + p.getType() + "' for parameter '" + p.getName() + "'");
            }
            symbolTable.declare(p.getName(), new SymbolInfo(p.getType(), false));
        }
        ASTNode body = node.getBody();
        if (body instanceof BlockNode block) {
            for (ASTNode statement : block.getChildren()) {
                analyzeStatement(statement);
            }
        } else {
            analyzeStatement(body);
        }
        symbolTable.exitScope();
        currentFunctionReturnType = previousReturnType;
    }
    private void analyzeStatement(ASTNode node) {
        if (node instanceof VarDeclNode var) {
            analyzeVarDeclaration(var);
        } else if (node instanceof AssignNode assign) {
            analyzeAssignment(assign);
        } else if (node instanceof IfNode ifNode) {
            analyzeIf(ifNode);
        } else if (node instanceof WhileNode whileNode) {
            analyzeWhile(whileNode);
        } else if (node instanceof ForNode forNode) {
            analyzeFor(forNode);
        } else if (node instanceof ReturnNode ret) {
            analyzeReturn(ret);
        } else if (node instanceof CallNode call) {
            analyzeCallExpr(call);
        } else if (node instanceof BlockNode block) {
            symbolTable.enterScope();
            for (ASTNode s : block.getChildren()) analyzeStatement(s);
            symbolTable.exitScope();
        }
    }
    private void analyzeVarDeclaration(VarDeclNode node) {
        String declaredType = node.getType();
        if (!symbolTable.isValidType(declaredType)) semanticError("TypeError", "Unknown type '" + declaredType + "'");
        if (node.getInitializer() != null) {
            String rhsType = getType(node.getInitializer());
            if (!typesCompatible(declaredType, rhsType)) semanticError("TypeError", "Cannot assign value of type '" + rhsType + "' to variable '" + node.getName() + "' of type '" + declaredType + "'");
        }
        symbolTable.declare(node.getName(), new SymbolInfo(declaredType, node.isFinal()));
    }
    private void analyzeAssignment(AssignNode node) {
        ASTNode target = node.getTarget();
        String varName = null;
        if (target instanceof IdentifierNode id) {
            varName = id.getName();
        } else if (target instanceof ArrayAccessNode arr) {
            if (arr.getArray() instanceof IdentifierNode id2) varName = id2.getName();
        }
        if (varName != null) {
            SymbolInfo info = symbolTable.lookup(varName);
            if (info.isFinal) semanticError("TypeError", "Cannot reassign final variable '" + varName + "'");
        }
        String rhsType = getType(node.getValue());

        if (target instanceof IdentifierNode id) {
            SymbolInfo info = symbolTable.lookup(id.getName());
            if (!typesCompatible(info.type, rhsType)) semanticError("TypeError", "Cannot assign value of type '" + rhsType + "' to variable '" + id.getName() + "' of type '" + info.type + "'");
        } else if (target instanceof ArrayAccessNode arr) {
            String arrType = getType(arr.getArray());
            if (!arrType.endsWith("[]")) semanticError("TypeError", "'" + arrType + "' is not an array type");
            String elemType = arrType.substring(0, arrType.length() - 2);
            if (!typesCompatible(elemType, rhsType)) semanticError("TypeError", "Cannot assign value of type '" + rhsType + "' to array of element type '" + elemType + "'");
        } else if (target instanceof FieldAccessNode field) {
            String objType = getType(field.getObject());
            String base    = objType.endsWith("[]") ? objType.substring(0, objType.length()-2) : objType;
            if (!symbolTable.isCollectionType(base)) semanticError("TypeError", "'" + objType + "' is not a collection, cannot access field '" + field.getField() + "'");
            String fieldType = symbolTable.getFieldType(base, field.getField());
            if (!typesCompatible(fieldType, rhsType)) semanticError("TypeError", "Cannot assign value of type '" + rhsType + "' to field '" + field.getField() + "' of type '" + fieldType + "'");
        }
    }
    private void analyzeIf(IfNode node) {
        String condType = getType(node.getCondition());
        if (!condType.equals("BOOL")) semanticError("MissingConditionError", "Condition of 'if' must be BOOL, got '" + condType + "'");
        symbolTable.enterScope();
        ASTNode thenBranch = node.getThenBranch();
        if (thenBranch instanceof BlockNode block)
            for (ASTNode s : block.getChildren()) analyzeStatement(s);
        else analyzeStatement(thenBranch);
        symbolTable.exitScope();
        ASTNode elseBranch = node.getElseBranch();
        if (elseBranch != null) {
            symbolTable.enterScope();
            if (elseBranch instanceof BlockNode block)
                for (ASTNode s : block.getChildren()) analyzeStatement(s);
            else analyzeStatement(elseBranch);
            symbolTable.exitScope();
        }
    }
    private void analyzeWhile(WhileNode node) {
        String condType = getType(node.getCondition());
        if (!condType.equals("BOOL")) semanticError("MissingConditionError", "Condition of 'while' must be BOOL, got '" + condType + "'");
        symbolTable.enterScope();
        ASTNode body = node.getBody();
        if (body instanceof BlockNode block)
            for (ASTNode s : block.getChildren()) analyzeStatement(s);
        else analyzeStatement(body);
        symbolTable.exitScope();
    }

    private void analyzeFor(ForNode node) {
        symbolTable.enterScope();
        ASTNode loopVar = node.getLoopVar();
        String varType = "";

        if (loopVar instanceof VarDeclNode varDecl) {
            analyzeVarDeclaration(varDecl);
            varType = varDecl.getType();
        } else if (loopVar instanceof IdentifierNode idNode) {
            varType = symbolTable.lookup(idNode.getName()).type;
        }
        if (!isNumeric(varType)) {
            semanticError("TypeError", "Loop variable must be numeric, got '" + varType + "'");
        }

        if (node.getStart() != null) {
            String startType = getType(node.getStart());
            if (!isNumeric(startType)) semanticError("TypeError", "For loop start must be numeric");
        }
        if (node.getEnd() != null) {
            String endType = getType(node.getEnd());
            if (!isNumeric(endType)) semanticError("TypeError", "For loop end must be numeric");
        }
 
        ASTNode body = node.getBody();
        if (body instanceof BlockNode block)
            for (ASTNode s : block.getChildren()) analyzeStatement(s);
        else analyzeStatement(body);
        
        symbolTable.exitScope();
    }
    private void analyzeReturn(ReturnNode node) {
        if (currentFunctionReturnType == null) semanticError("ReturnError", "'return' used outside of a function");
        boolean isVoid = currentFunctionReturnType.equals("void")
                || currentFunctionReturnType.isEmpty();
        if (node.getValue() == null) {
            if (!isVoid) semanticError("ReturnError", "Function must return '" + currentFunctionReturnType + "' but returns nothing");
        } else {
            if (isVoid) semanticError("ReturnError", "Void function must not return a value");
            String retType = getType(node.getValue());
            if (!typesCompatible(currentFunctionReturnType, retType)) semanticError("ReturnError", "Function must return '" + currentFunctionReturnType + "' but returns '" + retType + "'");
        }
    }
    private String getType(ASTNode node) {
        if (node instanceof IntLiteralNode)    return "INT";
        if (node instanceof FloatLiteralNode)  return "FLOAT";
        if (node instanceof BoolLiteralNode)   return "BOOL";
        if (node instanceof StringLiteralNode) return "STRING";
        if (node instanceof IdentifierNode id)
            return symbolTable.lookup(id.getName()).type;
        if (node instanceof ArrayAccessNode arr) {
            String indexType = getType(arr.getIndex());
            if (!indexType.equals("INT")) semanticError("TypeError", "Array index must be INT, got '" + indexType + "'");
            String arrType = getType(arr.getArray());
            if (!arrType.endsWith("[]")) semanticError("TypeError", "'" + arrType + "' is not an array type");
            return arrType.substring(0, arrType.length() - 2); // "INT[]" → "INT"
        }
        if (node instanceof ArrayConstructorNode arr) {
            String sizeType = getType(arr.getSize());
            if (!sizeType.equals("INT")) semanticError("TypeError", "Array size must be INT, got '" + sizeType + "'");
            return arr.getElementType() + "[]";
        }
        if (node instanceof BinaryOpNode bin)  return analyzeBinaryOp(bin);
        if (node instanceof UnaryOpNode un)    return analyzeUnaryOp(un);
        if (node instanceof CallNode call)     return analyzeCallExpr(call);
        if (node instanceof FieldAccessNode field) {
            String objType = getType(field.getObject());
            String base    = objType.endsWith("[]") ? objType.substring(0, objType.length()-2) : objType;
            if (!symbolTable.isCollectionType(base)) semanticError("TypeError", "'" + objType + "' is not a collection, cannot access field '" + field.getField() + "'");
            return symbolTable.getFieldType(base, field.getField());
        }
        if (node instanceof MethodCallNode method) {
            getType(method.getObject());
            return "void";
        }
        throw new RuntimeException("TypeError: Cannot determine type of node: " + node.getClass().getSimpleName());
    }
    private String analyzeBinaryOp(BinaryOpNode node) {
        String left  = getType(node.getLeft());
        String right = getType(node.getRight());
        String op    = node.getOperator();
        switch (op) {
            case "+" -> {
                if (left.equals("STRING") || right.equals("STRING")) {
                    return "STRING";
                }
                if (!isNumeric(left) || !isNumeric(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires numeric types or STRING concatenation, got '" + left + "' and '" + right + "'");
                }
                if (!left.equals(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires same numeric type on both sides, got '" + left + "' and '" + right + "'");
                }
                return left;
            }

            case "-", "*", "/", "%" -> {
                if (!isNumeric(left) || !isNumeric(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires numeric types, got '" + left + "' and '" + right + "'");
                }
                if (!left.equals(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires same numeric type on both sides, got '" + left + "' and '" + right + "'");
                }
                return left;
            }
            case "<", ">", "<=", ">=" -> {
                if (!isNumeric(left) || !isNumeric(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires numeric types, got '" + left + "' and '" + right + "'");
                }
                if (!left.equals(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires same numeric type on both sides, got '" + left + "' and '" + right + "'");
                }
                return "BOOL";
            }
            case "==", "=/=" -> {
                if (!left.equals(right)) {
                    semanticError("OperatorError", "Operator '" + op + "' requires same types, got '" + left + "' and '" + right + "'");
                }
                return "BOOL";
            }
            case "&&", "||" -> {
                if (!left.equals("BOOL") || !right.equals("BOOL")) {
                    semanticError("OperatorError", "Operator '" + op + "' requires BOOL operands, got '" + left + "' and '" + right + "'");
                }
                return "BOOL";
            }
            default -> throw new RuntimeException("OperatorError: Unknown binary operator '" + op + "'");
        }
    }
    private String analyzeUnaryOp(UnaryOpNode node) {
        String operandType = getType(node.getOperand());
        String op          = node.getOperator();
        return switch (op) {
            case "not" -> {
                if (!operandType.equals("BOOL")) semanticError("OperatorError", "'not' requires a BOOL operand, got '" + operandType + "'");
                yield "BOOL";
            }
            case "-" -> {
                if (!isNumeric(operandType)) semanticError("OperatorError", "Unary '-' requires a numeric operand, got '" + operandType + "'");
                yield operandType;
            }
            default -> throw new RuntimeException("OperatorError: Unknown unary operator '" + op + "'");
        };
    }
    private String analyzeCallExpr(CallNode node) {
        String name = node.getName();
        List<ASTNode> args = node.getArguments();

        if (Character.isUpperCase(name.charAt(0))) {
            if (!symbolTable.isCollectionType(name)) {
                semanticError("ArgumentError", "Unknown collection '" + name + "'");
            }
            
            List<String> expectedTypes = symbolTable.getCollectionFieldTypes(name);
            
            if (args.size() != expectedTypes.size()) {
                semanticError("ArgumentError", "Constructor of '" + name + "' expects " + expectedTypes.size() + " argument(s), got " + args.size());
            }
            
            for (int i = 0; i < args.size(); i++) {
                String argType = getType(args.get(i));
                if (!typesCompatible(expectedTypes.get(i), argType)) {
                    semanticError("ArgumentError", "Argument " + (i + 1) + " of constructor '" + name + "' expects '" + expectedTypes.get(i) + "', got '" + argType + "'");
                }
            }
            return name; 
        }

       
        SymbolInfo info = symbolTable.lookup(name);
        if (!info.isFunction) {
            semanticError("ArgumentError", "'" + name + "' is not a function");
        }

        List<String> expected = info.paramTypes;

        // "ANY" pour print"
        if (!expected.isEmpty() && expected.get(0).equals("ANY")) {
            // print/println/length --> 1 arg.
            if (args.size() != 1) {
                semanticError("ArgumentError", "Function '" + name + "' expects exactly 1 argument, got " + args.size());
            }
            getType(args.get(0)); 
        } 
        else {
            if (args.size() != expected.size()) {
                semanticError("ArgumentError", "Function '" + name + "' expects " + expected.size() + " argument(s), got " + args.size());
            }
            
            for (int i = 0; i < args.size(); i++) {
                String argType = getType(args.get(i));
                if (!typesCompatible(expected.get(i), argType)) {
                    semanticError("ArgumentError", "Argument " + (i + 1) + " of '" + name + "' expects '" + expected.get(i) + "', got '" + argType + "'");
                }
            }
        }

        return info.returnType != null ? info.returnType : "void";
    }
    //helper
    private boolean isNumeric(String type) {
        return type.equals("INT") || type.equals("FLOAT");
    }
    private boolean typesCompatible(String expected, String actual) {
        if ("ANY".equals(expected)) return true;
        if (expected.equals("FLOAT") && actual.equals("INT")) return true; // Promotion !
        return expected.equals(actual);
    }
    //exception
    public static class SemanticException extends RuntimeException {
        private final String keyword;
        public SemanticException(String keyword, String message) {
            super("[" + keyword + "] " + message);
            this.keyword = keyword;
        }
        public String getKeyword() { return keyword; }
    }
    private static void semanticError(String keyword, String message) {
        throw new SemanticException(keyword, message);
    }
}