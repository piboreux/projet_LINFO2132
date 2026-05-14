package compiler.codegen;

import compiler.parser.AST.*;
import compiler.parser.AST.literals.*;
import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import static org.objectweb.asm.Opcodes.*;

public class CodeGenerator {

    private final String mainClassName;
    private final String outputDir;

    private final Map<String, LinkedHashMap<String, String>> collDefs = new LinkedHashMap<>();
    private final Map<String, String> globalTypes = new LinkedHashMap<>();
    private final Map<String, FuncDeclNode> funcDecls = new LinkedHashMap<>();

    private ClassWriter  currentCw;
    private MethodVisitor mv;

    private Map<String, Integer> localSlots;
    private Map<String, String>  localTypes;
    private int nextSlot;

    private String currentReturnType;


    public CodeGenerator(String mainClassName, String outputDir) {
        this.mainClassName = mainClassName;
        this.outputDir     = outputDir;
    }

    //entry
    public void generate(ProgramNode program) throws IOException {
        // setup
        for (ASTNode node : program.getChildren()) {
            if (node instanceof CollDeclNode coll) {
                LinkedHashMap<String, String> fields = new LinkedHashMap<>();
                for (VarDeclNode f : coll.getFields()) fields.put(f.getName(), f.getType());
                collDefs.put(coll.getName(), fields);
            } else if (node instanceof FuncDeclNode fn) {
                funcDecls.put(fn.getName(), fn);
            } else if (node instanceof VarDeclNode vd) {
                globalTypes.put(vd.getName(), vd.getType());
            }
        }

        // one class file per collection
        for (var entry : collDefs.entrySet()) {
            generateCollectionClass(entry.getKey(), entry.getValue());
        }

        //main class
        generateMainClass(program);
    }

    private void generateCollectionClass(String name, LinkedHashMap<String, String> fields) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC, name, null, "java/lang/Object", null);

        for (var e : fields.entrySet()) {
            cw.visitField(ACC_PUBLIC, e.getKey(), typeDesc(e.getValue()), null, null).visitEnd();
        }

        StringBuilder ctorDesc = new StringBuilder("(");
        for (String ft : fields.values()) ctorDesc.append(typeDesc(ft));
        ctorDesc.append(")V");

        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", ctorDesc.toString(), null, null);
        ctor.visitCode();

        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        int slot = 1;
        for (var e : fields.entrySet()) {
            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitVarInsn(loadOp(e.getValue()), slot);
            ctor.visitFieldInsn(PUTFIELD, name, e.getKey(), typeDesc(e.getValue()));
            slot++;
        }
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(-1, -1);
        ctor.visitEnd();

        cw.visitEnd();
        writeClass(name, cw.toByteArray());
    }

    private void generateMainClass(ProgramNode program) throws IOException {
        currentCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        currentCw.visit(V1_8, ACC_PUBLIC, mainClassName, null, "java/lang/Object", null);

        currentCw.visitField(ACC_PRIVATE | ACC_STATIC, "__scanner__", "Ljava/util/Scanner;", null, null).visitEnd();

        for (ASTNode node : program.getChildren()) {
            if (node instanceof VarDeclNode vd) {
                currentCw.visitField(ACC_PUBLIC | ACC_STATIC,
                        vd.getName(), typeDesc(vd.getType()), null, null).visitEnd();
            }
        }

        generateStaticInit(program);
        for (ASTNode node : program.getChildren()) {
            if (node instanceof FuncDeclNode fn) {
                generateFunction(fn);
            }
        }
        if (!funcDecls.containsKey("main")) { // vide(pas main)
            MethodVisitor m = currentCw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            m.visitCode();
            m.visitInsn(RETURN);
            m.visitMaxs(-1, -1);
            m.visitEnd();
        }
        currentCw.visitEnd();
        writeClass(mainClassName, currentCw.toByteArray());
    }

    private void generateStaticInit(ProgramNode program) {
        mv = currentCw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        localSlots = new HashMap<>();
        localTypes  = new HashMap<>();
        nextSlot    = 0;

        mv.visitTypeInsn(NEW, "java/util/Scanner");
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitFieldInsn(PUTSTATIC, mainClassName, "__scanner__", "Ljava/util/Scanner;");

        for (ASTNode node : program.getChildren()) {
            if (node instanceof VarDeclNode vd && vd.getInitializer() != null) {
                genExpr(vd.getInitializer(), vd.getType());
                mv.visitFieldInsn(PUTSTATIC, mainClassName, vd.getName(), typeDesc(vd.getType()));
            }
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void generateFunction(FuncDeclNode fn) {
        boolean isMain = fn.getName().equals("main") && fn.getParameters().isEmpty();

        String descriptor;
        if (isMain) {
            descriptor = "([Ljava/lang/String;)V";
        } else {
            StringBuilder sb = new StringBuilder("(");
            for (VarDeclNode p : fn.getParameters()) sb.append(typeDesc(p.getType()));
            sb.append(")").append(retTypeDesc(fn.getReturnType()));
            descriptor = sb.toString();
        }

        mv = currentCw.visitMethod(ACC_PUBLIC | ACC_STATIC, fn.getName(), descriptor, null, null);
        mv.visitCode();
        localSlots         = new HashMap<>();
        localTypes         = new HashMap<>();
        nextSlot           = 0;
        currentReturnType  = fn.getReturnType();

        if (isMain) {
            nextSlot = 1;
        } else {
            for (VarDeclNode p : fn.getParameters()) {
                localSlots.put(p.getName(), nextSlot);
                localTypes.put(p.getName(), p.getType());
                nextSlot++;
            }
        }
        genBlock(fn.getBody());
        if (fn.getReturnType().equals("void") || fn.getReturnType().isEmpty() || isMain) {
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void genBlock(ASTNode node) {
        if (node instanceof BlockNode b) {
            for (ASTNode s : b.getChildren()) genStmt(s);
        } else {
            genStmt(node);
        }
    }

    private void genStmt(ASTNode node) {
        if      (node instanceof VarDeclNode  vd)  genVarDecl(vd);
        else if (node instanceof AssignNode   an)  genAssign(an);
        else if (node instanceof IfNode       in)  genIf(in);
        else if (node instanceof WhileNode    wn)  genWhile(wn);
        else if (node instanceof ForNode      fn)  genFor(fn);
        else if (node instanceof ReturnNode   rn)  genReturn(rn);
        else if (node instanceof CallNode     cn)  genCallStmt(cn);
        else if (node instanceof BlockNode    bn)  { for (ASTNode s : bn.getChildren()) genStmt(s); }
        else throw new RuntimeException("CodeGen: unknown statement: " + node.getClass().getSimpleName());
    }


    private void genVarDecl(VarDeclNode vd) {
        int slot = nextSlot++;
        localSlots.put(vd.getName(), slot);
        localTypes.put(vd.getName(), vd.getType());
        if (vd.getInitializer() != null) {
            genExprAs(vd.getInitializer(), vd.getType());
        } else {
            genDefault(vd.getType());
        }
        mv.visitVarInsn(storeOp(vd.getType()), slot);
    }


    private void genAssign(AssignNode an) {
        ASTNode target = an.getTarget();
        if (target instanceof IdentifierNode id) {
            String t = resolveType(id.getName());
            genExpr(an.getValue(), t);
            storeVar(id.getName(), t);

        } else if (target instanceof ArrayAccessNode arr) {
            genExpr(arr.getArray(), null);
            genExpr(arr.getIndex(), "INT");
            String elem = arrayElem(arr.getArray());
            genExpr(an.getValue(), elem);
            mv.visitInsn(arrStoreOp(elem));

        } else if (target instanceof FieldAccessNode fa) {
            genExpr(fa.getObject(), null);
            String coll  = inferType(fa.getObject());
            String ftype = collDefs.get(coll).get(fa.getField());
            genExpr(an.getValue(), ftype);
            mv.visitFieldInsn(PUTFIELD, coll, fa.getField(), typeDesc(ftype));
        }
    }


    private void genIf(IfNode in) {
        Label elseL = new Label();
        Label endL  = new Label();
        genCond(in.getCondition(), elseL);
        genBlock(in.getThenBranch());

        if (in.getElseBranch() != null) {
            mv.visitJumpInsn(GOTO, endL);
            mv.visitLabel(elseL);
            genBlock(in.getElseBranch());
            mv.visitLabel(endL);
        } else {
            mv.visitLabel(elseL);
        }
    }


    private void genWhile(WhileNode wn) {
        Label startL = new Label();
        Label endL   = new Label();
        mv.visitLabel(startL);
        genCond(wn.getCondition(), endL);
        genBlock(wn.getBody());
        mv.visitJumpInsn(GOTO, startL);
        mv.visitLabel(endL);
    }

    private void genFor(ForNode fn) {
        String varName, varType;

        if (fn.getLoopVar() instanceof VarDeclNode vd) {
            varName = vd.getName();
            varType = vd.getType();
            int slot = nextSlot++;
            localSlots.put(varName, slot);
            localTypes.put(varName, varType);
            genExpr(fn.getStart(), varType);
            mv.visitVarInsn(storeOp(varType), slot);
        } else {
            IdentifierNode id = (IdentifierNode) fn.getLoopVar();
            varName = id.getName();
            varType = resolveType(varName);
            genExpr(fn.getStart(), varType);
            storeVar(varName, varType);
        }

        int endSlot = nextSlot++;
        genExpr(fn.getEnd(), varType);
        mv.visitVarInsn(storeOp(varType), endSlot);

        Label startL = new Label();
        Label endL   = new Label();

        mv.visitLabel(startL);

        // i < endTmp
        loadVar(varName, varType);
        mv.visitVarInsn(loadOp(varType), endSlot);
        if (varType.equals("FLOAT")) {
            mv.visitInsn(FCMPL);
            mv.visitJumpInsn(IFGE, endL);
        } else {
            mv.visitJumpInsn(IF_ICMPGE, endL);
        }

        genBlock(fn.getBody());

        genExpr(fn.getIncrement(), varType);
        storeVar(varName, varType);

        mv.visitJumpInsn(GOTO, startL);
        mv.visitLabel(endL);
    }

    private void genReturn(ReturnNode rn) {
        if (rn.getValue() == null) {
            mv.visitInsn(RETURN);
        } else {
            genExprAs(rn.getValue(), currentReturnType);
            mv.visitInsn(retOp(currentReturnType));
        }
    }


    private void genCallStmt(CallNode cn) {
        String name = cn.getName();
        String ret  = builtinRet(name);
        if (ret == null) {
            if (Character.isUpperCase(name.charAt(0)) && collDefs.containsKey(name)) {
                genCallExpr(cn, name);
                mv.visitInsn(POP);
                return;
            }
            FuncDeclNode fn = funcDecls.get(name);
            ret = (fn != null) ? fn.getReturnType() : "void";
        }
        genCallExpr(cn, ret);
        if (!ret.equals("void") && !ret.isEmpty()) {
            mv.visitInsn(POP);
        }
    }

    private void genExpr(ASTNode node, String hint) {
        if (node instanceof IntLiteralNode    il) { mv.visitLdcInsn(Integer.parseInt(il.getValue())); }
        else if (node instanceof FloatLiteralNode fl) { mv.visitLdcInsn(Float.parseFloat(fl.getValue())); }
        else if (node instanceof BoolLiteralNode  bl) { mv.visitLdcInsn(bl.getValue().equals("true") ? 1 : 0); }
        else if (node instanceof StringLiteralNode sl) { mv.visitLdcInsn(sl.getValue()); }
        else if (node instanceof IdentifierNode   id) { String t = resolveType(id.getName()); loadVar(id.getName(), t); }
        else if (node instanceof BinaryOpNode     bn) { genBinary(bn); }
        else if (node instanceof UnaryOpNode      un) { genUnary(un); }
        else if (node instanceof CallNode         cn) { genCallExpr(cn, hint != null ? hint : inferType(cn)); }
        else if (node instanceof ArrayAccessNode  aa) {
            genExpr(aa.getArray(), null);
            genExpr(aa.getIndex(), "INT");
            mv.visitInsn(arrLoadOp(arrayElem(aa.getArray())));
        }
        else if (node instanceof ArrayConstructorNode ac) {
            genExpr(ac.getSize(), "INT");
            String elem = ac.getElementType();
            if (elem.equals("STRING") || collDefs.containsKey(elem)) {
                mv.visitTypeInsn(ANEWARRAY, elem.equals("STRING") ? "java/lang/String" : elem);
            } else {
                mv.visitIntInsn(NEWARRAY, arrPrimCode(elem));
            }
        }
        else if (node instanceof FieldAccessNode  fa) {
            genExpr(fa.getObject(), null);
            String coll  = inferType(fa.getObject());
            String ftype = collDefs.get(coll).get(fa.getField());
            mv.visitFieldInsn(GETFIELD, coll, fa.getField(), typeDesc(ftype));
        }
        else throw new RuntimeException("CodeGen: unknown expr node: " + node.getClass().getSimpleName());
    }


    private void genBinary(BinaryOpNode bin) {
        String op     = bin.getOperator();
        String ltype  = inferType(bin.getLeft());
        switch (op) {
            case "+" -> {
                String rtype = inferType(bin.getRight());

                if (ltype.equals("STRING") || rtype.equals("STRING")) {
                    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKESPECIAL,
                            "java/lang/StringBuilder",
                            "<init>",
                            "()V",
                            false);

                    genExpr(bin.getLeft(), ltype);
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            stringAppendDesc(ltype),
                            false);

                    genExpr(bin.getRight(), rtype);
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "append",
                            stringAppendDesc(rtype),
                            false);

                    mv.visitMethodInsn(INVOKEVIRTUAL,
                            "java/lang/StringBuilder",
                            "toString",
                            "()Ljava/lang/String;",
                            false);
                } else {
                    genArith(bin, ltype, IADD, FADD);
                }
            }
            case "-"  -> genArith(bin, ltype, ISUB, FSUB);
            case "*"  -> genArith(bin, ltype, IMUL, FMUL);
            case "/"  -> genArith(bin, ltype, IDIV, FDIV);
            case "%"  -> genArith(bin, ltype, IREM, FREM);
            case "&&" -> genAnd(bin);
            case "||" -> genOr(bin);
            case "<", ">", "<=", ">=", "==", "=/=" -> genCompare(bin, op, ltype);
            default -> throw new RuntimeException("CodeGen: unknown op: " + op);
        }
    }

    private void genArith(BinaryOpNode bin, String type, int iop, int fop) {
        genExpr(bin.getLeft(),  type);
        genExpr(bin.getRight(), type);
        mv.visitInsn(type.equals("FLOAT") ? fop : iop);
    }

    private void genAnd(BinaryOpNode bin) {
        Label falseL = new Label(), endL = new Label();
        genExpr(bin.getLeft(),  "BOOL");
        mv.visitJumpInsn(IFEQ, falseL);
        genExpr(bin.getRight(), "BOOL");
        mv.visitJumpInsn(IFEQ, falseL);
        mv.visitInsn(ICONST_1); mv.visitJumpInsn(GOTO, endL);
        mv.visitLabel(falseL);  mv.visitInsn(ICONST_0);
        mv.visitLabel(endL);
    }

    private void genOr(BinaryOpNode bin) {
        Label trueL = new Label(), endL = new Label();
        genExpr(bin.getLeft(),  "BOOL");
        mv.visitJumpInsn(IFNE, trueL);
        genExpr(bin.getRight(), "BOOL");
        mv.visitJumpInsn(IFNE, trueL);
        mv.visitInsn(ICONST_0); mv.visitJumpInsn(GOTO, endL);
        mv.visitLabel(trueL);   mv.visitInsn(ICONST_1);
        mv.visitLabel(endL);
    }

    private void genCompare(BinaryOpNode bin, String op, String ltype) {
        Label trueL = new Label(), endL = new Label();
        genExpr(bin.getLeft(),  ltype);
        genExpr(bin.getRight(), ltype);

        if (ltype.equals("INT") || ltype.equals("BOOL")) {
            mv.visitJumpInsn(intCmpTrue(op), trueL);
        } else if (ltype.equals("FLOAT")) {
            mv.visitInsn(FCMPL);
            mv.visitJumpInsn(floatCmpTrue(op), trueL);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals",
                    "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(op.equals("=/=") ? IFEQ : IFNE, trueL);
        }
        mv.visitInsn(ICONST_0); mv.visitJumpInsn(GOTO, endL);
        mv.visitLabel(trueL);   mv.visitInsn(ICONST_1);
        mv.visitLabel(endL);
    }

    private void genUnary(UnaryOpNode un) {
        String type = inferType(un.getOperand());
        switch (un.getOperator()) {
            case "not" -> {
                genExpr(un.getOperand(), "BOOL");
                Label t = new Label(), e = new Label();
                mv.visitJumpInsn(IFEQ, t);
                mv.visitInsn(ICONST_0); mv.visitJumpInsn(GOTO, e);
                mv.visitLabel(t); mv.visitInsn(ICONST_1);
                mv.visitLabel(e);
            }
            case "-" -> {
                genExpr(un.getOperand(), type);
                mv.visitInsn(type.equals("FLOAT") ? FNEG : INEG);
            }
        }
    }

    private void genCallExpr(CallNode call, String expectedType) {
        String name = call.getName();
        switch (name) {
            case "println" -> {
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                ASTNode a = call.getArguments().get(0);
                String  t = inferType(a);
                genExpr(a, t);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream",
                        "println", printDesc(t), false);
                return;
            }
            case "print" -> {
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                ASTNode a = call.getArguments().get(0);
                String  t = inferType(a);
                genExpr(a, t);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream",
                        "print", printDesc(t), false);
                return;
            }
            case "print_INT" -> {
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                genExpr(call.getArguments().get(0), "INT");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
                return;
            }
            case "print_FLOAT" -> {
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                genExpr(call.getArguments().get(0), "FLOAT");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(F)V", false);
                return;
            }
            case "read_INT" -> {
                mv.visitFieldInsn(GETSTATIC, mainClassName, "__scanner__", "Ljava/util/Scanner;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false);
                return;
            }
            case "read_FLOAT" -> {
                mv.visitFieldInsn(GETSTATIC, mainClassName, "__scanner__", "Ljava/util/Scanner;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextFloat", "()F", false);
                return;
            }
            case "read_STRING" -> {
                mv.visitFieldInsn(GETSTATIC, mainClassName, "__scanner__", "Ljava/util/Scanner;");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextLine",
                        "()Ljava/lang/String;", false);
                return;
            }
            case "str" -> {
                genExpr(call.getArguments().get(0), "INT");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString",
                        "(I)Ljava/lang/String;", false);
                return;
            }
            case "floor" -> {
                genExpr(call.getArguments().get(0), "FLOAT");
                mv.visitInsn(F2D);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
                mv.visitInsn(D2I);
                return;
            }
            case "ceil" -> {
                genExpr(call.getArguments().get(0), "FLOAT");
                mv.visitInsn(F2D);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
                mv.visitInsn(D2I);
                return;
            }
            case "length" -> {
                ASTNode a = call.getArguments().get(0);
                String  t = inferType(a);
                genExpr(a, t);
                if (t.equals("STRING")) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
                } else {
                    mv.visitInsn(ARRAYLENGTH);
                }
                return;
            }
            case "not" -> {
                genExpr(call.getArguments().get(0), "BOOL");
                Label t = new Label(), e = new Label();
                mv.visitJumpInsn(IFEQ, t);
                mv.visitInsn(ICONST_0); mv.visitJumpInsn(GOTO, e);
                mv.visitLabel(t); mv.visitInsn(ICONST_1);
                mv.visitLabel(e);
                return;
            }
        }
        if (Character.isUpperCase(name.charAt(0)) && collDefs.containsKey(name)) {
            mv.visitTypeInsn(NEW, name);
            mv.visitInsn(DUP);
            LinkedHashMap<String, String> fields = collDefs.get(name);
            List<String>  ftypes = new ArrayList<>(fields.values());
            List<ASTNode> args   = call.getArguments();
            for (int i = 0; i < args.size(); i++) genExpr(args.get(i), ftypes.get(i));
            StringBuilder d = new StringBuilder("(");
            for (String ft : ftypes) d.append(typeDesc(ft));
            d.append(")V");
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", d.toString(), false);
            return;
        }
        FuncDeclNode fn = funcDecls.get(name);
        if (fn == null) throw new RuntimeException("CodeGen: unknown function: " + name);

        List<ASTNode>   args   = call.getArguments();
        List<VarDeclNode> pms  = fn.getParameters();
        StringBuilder   desc   = new StringBuilder("(");
        for (int i = 0; i < args.size(); i++) {
            String pt = pms.get(i).getType();
            genExpr(args.get(i), pt);
            desc.append(typeDesc(pt));
        }
        desc.append(")").append(retTypeDesc(fn.getReturnType()));
        mv.visitMethodInsn(INVOKESTATIC, mainClassName, name, desc.toString(), false);
    }


    private void genCond(ASTNode cond, Label jumpWhenFalse) {
        if (cond instanceof BinaryOpNode bin && isCmpOp(bin.getOperator())) {
            String ltype = inferType(bin.getLeft());
            genExpr(bin.getLeft(),  ltype);
            genExpr(bin.getRight(), ltype);
            if (ltype.equals("INT") || ltype.equals("BOOL")) {
                mv.visitJumpInsn(intCmpFalse(bin.getOperator()), jumpWhenFalse);
            } else if (ltype.equals("FLOAT")) {
                mv.visitInsn(FCMPL);
                mv.visitJumpInsn(floatCmpFalse(bin.getOperator()), jumpWhenFalse);
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals",
                        "(Ljava/lang/Object;)Z", false);
                mv.visitJumpInsn(bin.getOperator().equals("=/=") ? IFNE : IFEQ, jumpWhenFalse);
            }
        } else {
            genExpr(cond, "BOOL");
            mv.visitJumpInsn(IFEQ, jumpWhenFalse);
        }
    }

    private String inferType(ASTNode node) {
        if (node instanceof IntLiteralNode)    return "INT";
        if (node instanceof FloatLiteralNode)  return "FLOAT";
        if (node instanceof BoolLiteralNode)   return "BOOL";
        if (node instanceof StringLiteralNode) return "STRING";
        if (node instanceof IdentifierNode id) return resolveType(id.getName());
        if (node instanceof BinaryOpNode   bn) {
            return isCmpOp(bn.getOperator()) || bn.getOperator().equals("&&") || bn.getOperator().equals("||")
                    ? "BOOL" : inferType(bn.getLeft());
        }
        if (node instanceof UnaryOpNode    un) {
            return un.getOperator().equals("not") ? "BOOL" : inferType(un.getOperand());
        }
        if (node instanceof CallNode       cn) return inferCallType(cn.getName());
        if (node instanceof ArrayAccessNode aa) {
            String at = inferType(aa.getArray());
            return at.substring(0, at.length() - 2);
        }
        if (node instanceof ArrayConstructorNode ac) return ac.getElementType() + "[]";
        if (node instanceof FieldAccessNode fa) {
            String obj = inferType(fa.getObject());
            return collDefs.get(obj).get(fa.getField());
        }
        throw new RuntimeException("CodeGen: can't infer type of " + node.getClass().getSimpleName());
    }

    private String inferCallType(String name) {
        String b = builtinRet(name);
        if (b != null) return b;
        if (Character.isUpperCase(name.charAt(0)) && collDefs.containsKey(name)) return name;
        FuncDeclNode fn = funcDecls.get(name);
        return fn != null ? fn.getReturnType() : "void";
    }

    private String builtinRet(String name) {
        return switch (name) {
            case "println", "print", "print_INT", "print_FLOAT" -> "void";
            case "read_INT"    -> "INT";
            case "read_FLOAT"  -> "FLOAT";
            case "read_STRING" -> "STRING";
            case "str"         -> "STRING";
            case "floor", "ceil", "length" -> "INT";
            case "not"         -> "BOOL";
            default -> null;
        };
    }

    private String resolveType(String name) {
        if (localTypes.containsKey(name))  return localTypes.get(name);
        if (globalTypes.containsKey(name)) return globalTypes.get(name);
        throw new RuntimeException("CodeGen: unknown variable '" + name + "'");
    }

    private void loadVar(String name, String type) {
        if (localSlots.containsKey(name)) {
            mv.visitVarInsn(loadOp(type), localSlots.get(name));
        } else {
            mv.visitFieldInsn(GETSTATIC, mainClassName, name, typeDesc(type));
        }
    }

    private void storeVar(String name, String type) {
        if (localSlots.containsKey(name)) {
            mv.visitVarInsn(storeOp(type), localSlots.get(name));
        } else {
            mv.visitFieldInsn(PUTSTATIC, mainClassName, name, typeDesc(type));
        }
    }

    private void genDefault(String type) {
        switch (type) {
            case "INT", "BOOL" -> mv.visitInsn(ICONST_0);
            case "FLOAT"       -> mv.visitInsn(FCONST_0);
            case "STRING"      -> mv.visitLdcInsn("");
            default            -> mv.visitInsn(ACONST_NULL);
        }
    }

    private String arrayElem(ASTNode arrNode) {
        String t = inferType(arrNode);
        return t.substring(0, t.length() - 2);
    }

    private String typeDesc(String type) {
        if (type.endsWith("[]")) return "[" + typeDesc(type.substring(0, type.length() - 2));
        return switch (type) {
            case "INT"    -> "I";
            case "FLOAT"  -> "F";
            case "BOOL"   -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "void"   -> "V";
            default       -> "L" + type + ";";
        };
    }

    private String retTypeDesc(String type) {
        if (type == null || type.isEmpty() || type.equals("void")) return "V";
        return typeDesc(type);
    }

    private String printDesc(String type) {
        return switch (type) {
            case "INT"    -> "(I)V";
            case "FLOAT"  -> "(F)V";
            case "BOOL"   -> "(Z)V";
            case "STRING" -> "(Ljava/lang/String;)V";
            default       -> "(Ljava/lang/Object;)V";
        };
    }


    private String stringAppendDesc(String type) {
        return switch (type) {
            case "INT"    -> "(I)Ljava/lang/StringBuilder;";
            case "FLOAT"  -> "(F)Ljava/lang/StringBuilder;";
            case "BOOL"   -> "(Z)Ljava/lang/StringBuilder;";
            case "STRING" -> "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
            default       -> "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
        };
    }

    private int loadOp(String type) {
        return switch (type) {
            case "INT", "BOOL" -> ILOAD;
            case "FLOAT"       -> FLOAD;
            default            -> ALOAD;
        };
    }

    private int storeOp(String type) {
        return switch (type) {
            case "INT", "BOOL" -> ISTORE;
            case "FLOAT"       -> FSTORE;
            default            -> ASTORE;
        };
    }

    private int retOp(String type) {
        return switch (type) {
            case "INT", "BOOL" -> IRETURN;
            case "FLOAT"       -> FRETURN;
            case "void", ""    -> RETURN;
            default            -> ARETURN;
        };
    }

    private int arrLoadOp(String elem) {
        return switch (elem) {
            case "INT"   -> IALOAD;
            case "FLOAT" -> FALOAD;
            case "BOOL"  -> BALOAD;
            default      -> AALOAD;
        };
    }

    private int arrStoreOp(String elem) {
        return switch (elem) {
            case "INT"   -> IASTORE;
            case "FLOAT" -> FASTORE;
            case "BOOL"  -> BASTORE;
            default      -> AASTORE;
        };
    }

    private int arrPrimCode(String elem) {
        return switch (elem) {
            case "INT"   -> T_INT;
            case "FLOAT" -> T_FLOAT;
            case "BOOL"  -> T_BOOLEAN;
            default -> throw new RuntimeException("Not a primitive array elem: " + elem);
        };
    }

    private boolean isCmpOp(String op) {
        return switch (op) { case "<","<=",">",">=","==","=/=" -> true; default -> false; };
    }

    private int intCmpTrue(String op) {
        return switch (op) {
            case "<"   -> IF_ICMPLT;
            case ">"   -> IF_ICMPGT;
            case "<="  -> IF_ICMPLE;
            case ">="  -> IF_ICMPGE;
            case "=="  -> IF_ICMPEQ;
            case "=/=" -> IF_ICMPNE;
            default -> throw new RuntimeException("Bad cmp op: " + op);
        };
    }

    private int intCmpFalse(String op) {
        return switch (op) {
            case "<"   -> IF_ICMPGE;
            case ">"   -> IF_ICMPLE;
            case "<="  -> IF_ICMPGT;
            case ">="  -> IF_ICMPLT;
            case "=="  -> IF_ICMPNE;
            case "=/=" -> IF_ICMPEQ;
            default -> throw new RuntimeException("Bad cmp op: " + op);
        };
    }

    private int floatCmpTrue(String op) {
        return switch (op) {
            case "<"   -> IFLT;
            case ">"   -> IFGT;
            case "<="  -> IFLE;
            case ">="  -> IFGE;
            case "=="  -> IFEQ;
            case "=/=" -> IFNE;
            default -> throw new RuntimeException("Bad cmp op: " + op);
        };
    }

    private int floatCmpFalse(String op) {
        return switch (op) {
            case "<"   -> IFGE;
            case ">"   -> IFLE;
            case "<="  -> IFGT;
            case ">="  -> IFLT;
            case "=="  -> IFNE;
            case "=/=" -> IFEQ;
            default -> throw new RuntimeException("Bad cmp op: " + op);
        };
    }

    private void writeClass(String className, byte[] bytes) throws IOException {
        File out = new File(outputDir, className + ".class");
        out.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(bytes);
        }
    }

    private void genExprAs(ASTNode node, String expectedType) {
        String actualType = inferType(node);
        genExpr(node, expectedType);

        if ("FLOAT".equals(expectedType) && "INT".equals(actualType)) {
            mv.visitInsn(I2F);
        }
    }
}