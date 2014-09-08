package com.xseagullx.groovy.gsoc;

import com.xseagullx.groovy.gsoc.util.StringUtil;
import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.antlr.EnumHelper;
import org.codehaus.groovy.parser.antlr4.GroovyLexer;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ContinueStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.syntax.Numbers;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("ALL") public class ASTBuilder
{
    private static Logger log = Logger.getLogger("com.xseagullx.groovy.gsoc.ASTBuilder");

    public ASTBuilder(SourceUnit sourceUnit, ClassLoader classLoader) {
        instance = this;
        this.classLoader = classLoader;
        this.sourceUnit = sourceUnit;
        moduleNode = new ModuleNode(sourceUnit);


        String text = null;
        try {
            text = StringUtil.replaceHexEscapes(DefaultGroovyMethods.getText(sourceUnit.getSource().getReader()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Smth went wrong.", e);
        }

        if (log.isLoggable(Level.FINE)) {
            final GroovyLexer lexer = new GroovyLexer(new ANTLRInputStream(text));
            log.fine(DefaultGroovyMethods.multiply("=", 60) + "\n" + text + "\n" + DefaultGroovyMethods.multiply("=", 60));
            log.fine("\nLexer TOKENS:\n\t" + DefaultGroovyMethods.join(DefaultGroovyMethods.collect(lexer.getAllTokens(), new Closure<String>(this, this) {
                public String doCall(Token it) { return String.valueOf(it.getLine()) + ", " + String.valueOf(it.getStartIndex()) + ":" + String.valueOf(it.getStopIndex()) + " " + GroovyLexer.tokenNames[it.getType()] + " " + it.getText(); }

                public String doCall() {
                    return doCall(null);
                }

            }), "\n\t") + DefaultGroovyMethods.multiply("=", 60));
        }


        GroovyLexer lexer = new GroovyLexer(new ANTLRInputStream(text));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        GroovyParser parser = new GroovyParser(tokens);
        ParseTree tree = parser.compilationUnit();
        if (log.isLoggable(Level.FINE)) {
            final StringBuffer s = DefaultGroovyMethods.leftShift("", "");
            new ParseTreeWalker().walk(new ParseTreeListener() {
                @Override public void visitTerminal(@NotNull TerminalNode node) {
                    DefaultGroovyMethods.leftShift(DefaultGroovyMethods.leftShift(s, (DefaultGroovyMethods.multiply(".\t", indent) + String.valueOf(node))), "\n");
                }

                @Override public void visitErrorNode(@NotNull ErrorNode node) {
                }

                @Override public void enterEveryRule(@NotNull final ParserRuleContext ctx) {
                    DefaultGroovyMethods.leftShift(DefaultGroovyMethods.leftShift(s, (DefaultGroovyMethods.multiply(".\t", indent) + GroovyParser.ruleNames[ctx.getRuleIndex()] + ": {")), "\n");
                    indent = indent++;
                }

                @Override public void exitEveryRule(@NotNull ParserRuleContext ctx) {
                    indent = indent--;
                    DefaultGroovyMethods.leftShift(DefaultGroovyMethods.leftShift(s, (DefaultGroovyMethods.multiply(".\t", indent) + "}")), "\n");
                }

                public int getIndent() {
                    return indent;
                }

                public void setIndent(int indent) {
                    this.indent = indent;
                }

                private int indent;
            }, tree);

            log.fine((DefaultGroovyMethods.multiply("=", 60)) + "\n" + String.valueOf(s) + "\n" + (DefaultGroovyMethods.multiply("=", 60)));
        }


        try {
            DefaultGroovyMethods.each(((GroovyParser.CompilationUnitContext)tree).importStatement(), new MethodClosure(this, "parseImportStatement"));
            DefaultGroovyMethods.each(((GroovyParser.CompilationUnitContext)tree).children, new Closure<ClassNode>(this, this) {
                public ClassNode doCall(ParseTree it) {
                    if (it instanceof GroovyParser.EnumDeclarationContext)
                        parseEnumDeclaration((GroovyParser.EnumDeclarationContext)it);
                    else if (it instanceof GroovyParser.ClassDeclarationContext)
                        return parseClassDeclaration((GroovyParser.ClassDeclarationContext)it);
                    else if (it instanceof GroovyParser.PackageDefinitionContext)
                        parsePackageDefinition((GroovyParser.PackageDefinitionContext)it);
                    return null;
                }

                public ClassNode doCall() {
                    return doCall(null);
                }

            });
            DefaultGroovyMethods.collect(((GroovyParser.CompilationUnitContext)tree).statement(), new Closure<Object>(this, this) {
                public void doCall(GroovyParser.StatementContext it) {
                    moduleNode.addStatement(parseStatement(it));
                }

                public void doCall() {
                    doCall(null);
                }

            });
        } catch (CompilationFailedException ignored) {
            // Compilation failed.
        }

    }

    public void parseImportStatement(@NotNull GroovyParser.ImportStatementContext ctx) {
        ImportNode node;
        if (ctx.getChild(ctx.getChildCount() - 1).getText().equals("*")) {
            moduleNode.addStarImport(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".") + ".");
            node = DefaultGroovyMethods.last(moduleNode.getStarImports());
        } else {
            moduleNode.addImport(DefaultGroovyMethods.last(ctx.IDENTIFIER()).getText(), ClassHelper.make(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".")), parseAnnotations(ctx.annotationClause()));
            node = DefaultGroovyMethods.last(moduleNode.getImports());
            setupNodeLocation(node.getType(), ctx);
        }

        setupNodeLocation(node, ctx);
    }

    public void parsePackageDefinition(@NotNull GroovyParser.PackageDefinitionContext ctx) {
        moduleNode.setPackageName(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".") + ".");
        attachAnnotations(moduleNode.getPackage(), ctx.annotationClause());
        setupNodeLocation(moduleNode.getPackage(), ctx);
    }

    public void parseEnumDeclaration(@NotNull GroovyParser.EnumDeclarationContext ctx) {
        List list = DefaultGroovyMethods.asBoolean(ctx.implementsClause())
                    ? DefaultGroovyMethods.collect(ctx.implementsClause().genericClassNameExpression(), new Closure<ClassNode>(this, this) {
            public ClassNode doCall(GroovyParser.GenericClassNameExpressionContext it) {return parseExpression(it);}

            public ClassNode doCall() {
                return doCall(null);
            }

        }) : new ArrayList();
        ClassNode[] interfaces = (ClassNode[])list.toArray(new ClassNode[list.size()]);
        final ClassNode classNode = EnumHelper.makeEnumNode(ctx.IDENTIFIER().getText(), Modifier.PUBLIC, interfaces, null);// FIXME merge with class declaration.
        setupNodeLocation(classNode, ctx);
        attachAnnotations(classNode, ctx.annotationClause());
        moduleNode.addClass(classNode);

        classNode.setModifiers(parseClassModifiers(ctx.classModifier()) | Opcodes.ACC_ENUM | Opcodes.ACC_FINAL);
        classNode.setSyntheticPublic((classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0);
        classNode.setModifiers(classNode.getModifiers() & ~Opcodes.ACC_SYNTHETIC);// FIXME Magic with synthetic modifier.

        List<TerminalNode> enumConstants = DefaultGroovyMethods.collect(DefaultGroovyMethods.grep(ctx.enumMember(), new Closure<TerminalNode>(this, this) {
            public TerminalNode doCall(GroovyParser.EnumMemberContext e) {return e.IDENTIFIER();}

        }), new Closure<TerminalNode>(this, this) {
            public TerminalNode doCall(GroovyParser.EnumMemberContext it) {return it.IDENTIFIER();}

            public TerminalNode doCall() {
                return doCall(null);
            }

        });
        List<GroovyParser.ClassMemberContext> classMembers = DefaultGroovyMethods.collect(DefaultGroovyMethods.grep(ctx.enumMember(), new Closure<GroovyParser.ClassMemberContext>(this, this) {
            public GroovyParser.ClassMemberContext doCall(GroovyParser.EnumMemberContext e) {return e.classMember();}

        }), new Closure<GroovyParser.ClassMemberContext>(this, this) {
            public GroovyParser.ClassMemberContext doCall(GroovyParser.EnumMemberContext it) {return it.classMember();}

            public GroovyParser.ClassMemberContext doCall() {
                return doCall(null);
            }

        });
        DefaultGroovyMethods.each(enumConstants, new Closure<FieldNode>(this, this) {
            public FieldNode doCall(TerminalNode it) {
                return setupNodeLocation(EnumHelper.addEnumConstant(classNode, it.getText(), null), it.getSymbol());
            }

            public FieldNode doCall() {
                return doCall(null);
            }

        });
        parseMembers(classNode, classMembers);
    }

    public ClassNode parseClassDeclaration(@NotNull final GroovyParser.ClassDeclarationContext ctx) {
        ClassNode classNode;
        final ClassNode parentClass = DefaultGroovyMethods.asBoolean(classes) ? classes.peek() : null;
        if (DefaultGroovyMethods.asBoolean(parentClass)) {
            String string = parentClass.getName() + "$" + String.valueOf(ctx.IDENTIFIER());
            classNode = new InnerClassNode(parentClass, string, Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
        } else {
            final String name = moduleNode.getPackageName();
            classNode = new ClassNode((name != null && DefaultGroovyMethods.asBoolean(name) ? name : "") + String.valueOf(ctx.IDENTIFIER()), Modifier.PUBLIC, ClassHelper.OBJECT_TYPE);
        }


        setupNodeLocation(classNode, ctx);
        attachAnnotations(classNode, ctx.annotationClause());
        moduleNode.addClass(classNode);
        if (DefaultGroovyMethods.asBoolean(ctx.extendsClause()))
            (classNode).setSuperClass(parseExpression(ctx.extendsClause().genericClassNameExpression()));
        if (DefaultGroovyMethods.asBoolean(ctx.implementsClause()))
            (classNode).setInterfaces(DefaultGroovyMethods.asType(DefaultGroovyMethods.collect(ctx.implementsClause().genericClassNameExpression(), new Closure<ClassNode>(this, this) {
                public ClassNode doCall(GroovyParser.GenericClassNameExpressionContext it) {return parseExpression(it);}

                public ClassNode doCall() {
                    return doCall(null);
                }

            }), ClassNode[].class));

        (classNode).setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));
        (classNode).setUsingGenerics((classNode.getGenericsTypes() != null && classNode.getGenericsTypes().length != 0) || (classNode).getSuperClass().isUsingGenerics() || DefaultGroovyMethods.any(classNode.getInterfaces(), new Closure<Boolean>(this, this) {
            public Boolean doCall(ClassNode it) {return it.isUsingGenerics();}

            public Boolean doCall() {
                return doCall(null);
            }

        }));
        classNode.setModifiers(parseClassModifiers(ctx.classModifier()) | (DefaultGroovyMethods.asBoolean(ctx.KW_INTERFACE())
                                                                           ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT
                                                                           : 0));
        classNode.setSyntheticPublic((classNode.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0);
        classNode.setModifiers(classNode.getModifiers() & ~Opcodes.ACC_SYNTHETIC);// FIXME Magic with synthetic modifier.

        if (DefaultGroovyMethods.asBoolean(ctx.AT())) {
            classNode.addInterface(ClassHelper.Annotation_TYPE);
            classNode.setModifiers(classNode.getModifiers() | Opcodes.ACC_ANNOTATION);
        }


        DefaultGroovyMethods.leftShift(classes, classNode);
        parseMembers(classNode, ctx.classBody().classMember());
        classes.pop();

        if (classNode.isInterface()) { // FIXME why interface has null mixin
            try {
                // FIXME Hack with visibility.
                Field field = classNode.getClass().getDeclaredField("mixins");
                field.setAccessible(true);
                field.set(classNode, null);
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return classNode;
    }

    public void parseMembers(ClassNode classNode, List<GroovyParser.ClassMemberContext> ctx) {
        for (GroovyParser.ClassMemberContext member : ctx) {
            ParseTree memberContext = DefaultGroovyMethods.last(member.children);

            ASTNode memberNode = null;
            if (memberContext instanceof GroovyParser.ClassDeclarationContext)
                memberNode = parseClassDeclaration(DefaultGroovyMethods.asType(memberContext, GroovyParser.ClassDeclarationContext.class));
            else if (memberContext instanceof GroovyParser.EnumDeclarationContext)
                parseEnumDeclaration(DefaultGroovyMethods.asType(memberContext, GroovyParser.EnumDeclarationContext.class));
            else if (memberContext instanceof GroovyParser.ConstructorDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.ConstructorDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.MethodDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.MethodDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.FieldDeclarationContext)
                memberNode = parseMember(classNode, (GroovyParser.FieldDeclarationContext)memberContext);
            else if (memberContext instanceof GroovyParser.ObjectInitializerContext)
                parseMember(classNode, (GroovyParser.ObjectInitializerContext)memberContext);
            else if (memberContext instanceof GroovyParser.ClassInitializerContext)
                parseMember(classNode, (GroovyParser.ClassInitializerContext)memberContext);
            else
                assert false : "Unknown class member type.";
            if (DefaultGroovyMethods.asBoolean(memberNode)) setupNodeLocation(memberNode, member);
            if (member.getChildCount() > 1) {
                assert memberNode != null;
                for (int i = 0; i < member.children.size() - 2; i++) {
                    ParseTree annotationCtx = member.children.get(i);
                    assert annotationCtx instanceof GroovyParser.AnnotationClauseContext;
                    ((AnnotatedNode)memberNode).addAnnotation(parseAnnotation((GroovyParser.AnnotationClauseContext)annotationCtx));
                }

            }

        }

    }

    @SuppressWarnings("GroovyUnusedDeclaration") public AnnotatedNode parseMember(ClassNode classNode, GroovyParser.MethodDeclarationContext ctx) {
        //noinspection GroovyAssignabilityCheck
        final Iterator<Object> iterator = parseModifiers(ctx.memberModifier(), Opcodes.ACC_PUBLIC).iterator();
        int modifiers = ((Integer)(iterator.hasNext() ? iterator.next() : null));
        boolean hasVisibilityModifier = ((Boolean)(iterator.hasNext() ? iterator.next() : null));

        DefaultGroovyMethods.leftShift(innerClassesDefinedInMethod, new ArrayList());
        Statement statement = DefaultGroovyMethods.asBoolean(ctx.methodBody())
                              ? parseStatement(DefaultGroovyMethods.asType(ctx.methodBody().blockStatement(), GroovyParser.BlockStatementContext.class))
                              : null;
        List<InnerClassNode> innerClassesDeclared = innerClassesDefinedInMethod.pop();

        Parameter[] params = parseParameters(ctx.argumentDeclarationList());

        ClassNode returnType = DefaultGroovyMethods.asBoolean(ctx.typeDeclaration())
                               ? parseTypeDeclaration(ctx.typeDeclaration())
                               : DefaultGroovyMethods.asBoolean(ctx.genericClassNameExpression())
                                 ? parseExpression(ctx.genericClassNameExpression())
                                 : ClassHelper.OBJECT_TYPE;

        ClassNode[] exceptions = parseThrowsClause(ctx.throwsClause());
        modifiers |= classNode.isInterface() ? Opcodes.ACC_ABSTRACT : 0;
        final MethodNode methodNode = classNode.addMethod(ctx.IDENTIFIER().getText(), modifiers, returnType, params, exceptions, statement);
        methodNode.setGenericsTypes(parseGenericDeclaration(ctx.genericDeclarationList()));
        DefaultGroovyMethods.each(innerClassesDeclared, new Closure<MethodNode>(this, this) {
            public MethodNode doCall(InnerClassNode it) {
                it.setEnclosingMethod(methodNode);
                return methodNode;
            }

            public MethodNode doCall() {
                return doCall(null);
            }

        });

        setupNodeLocation(methodNode, ctx);
        attachAnnotations(methodNode, ctx.annotationClause());
        methodNode.setSyntheticPublic(!hasVisibilityModifier);
        return methodNode;
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public AnnotatedNode parseMember(ClassNode classNode, GroovyParser.FieldDeclarationContext ctx) {
        //noinspection GroovyAssignabilityCheck
        final Iterator<Object> iterator = parseModifiers(ctx.memberModifier()).iterator();
        int modifiers = ((Integer)(iterator.hasNext() ? iterator.next() : null));
        boolean hasVisibilityModifier = ((Boolean)(iterator.hasNext() ? iterator.next() : null));

        modifiers |= classNode.isInterface() ? Opcodes.ACC_STATIC | Opcodes.ACC_FINAL : 0;


        GroovyParser.ExpressionContext initExprContext = ctx.expression();
        Expression initialierExpression = DefaultGroovyMethods.asBoolean(initExprContext)
                                          ? parseExpression(initExprContext)
                                          : null;
        ClassNode typeDeclaration = DefaultGroovyMethods.asBoolean(ctx.genericClassNameExpression())
                                    ? parseExpression(ctx.genericClassNameExpression())
                                    : ClassHelper.OBJECT_TYPE;
        AnnotatedNode node;
        Expression initialValue = classNode.isInterface() && !typeDeclaration.equals(ClassHelper.OBJECT_TYPE)
                                  ? new ConstantExpression(initialExpressionForType(typeDeclaration))
                                  : initialierExpression;
        if (classNode.isInterface() || hasVisibilityModifier) {
            modifiers |= classNode.isInterface() ? Opcodes.ACC_PUBLIC : 0;

            FieldNode field = classNode.addField(ctx.IDENTIFIER().getText(), modifiers, typeDeclaration, initialValue);
            attachAnnotations(field, ctx.annotationClause());
            node = setupNodeLocation(field, ctx);
        } else {// no visibility specified. Generate property node.
            Integer propertyModifier = modifiers | Opcodes.ACC_PUBLIC;
            PropertyNode propertyNode = classNode.addProperty(ctx.IDENTIFIER().getText(), propertyModifier, typeDeclaration, initialValue, null, null);
            propertyNode.getField().setModifiers(modifiers | Opcodes.ACC_PRIVATE);
            propertyNode.getField().setSynthetic(!classNode.isInterface());
            node = setupNodeLocation(propertyNode.getField(), ctx);
            attachAnnotations(propertyNode.getField(), ctx.annotationClause());
            setupNodeLocation(propertyNode, ctx);
        }

        return node;
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static void parseMember(ClassNode classNode, GroovyParser.ClassInitializerContext ctx) {
        (DefaultGroovyMethods.asType(getOrCreateClinitMethod(classNode).getCode(), BlockStatement.class)).addStatement(parseStatement(ctx.blockStatement()));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static void parseMember(ClassNode classNode, GroovyParser.ObjectInitializerContext ctx) {
        BlockStatement statement = new BlockStatement();
        statement.addStatement(parseStatement(ctx.blockStatement()));
        classNode.addObjectInitializerStatements(statement);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static AnnotatedNode parseMember(ClassNode classNode, GroovyParser.ConstructorDeclarationContext ctx) {
        int modifiers = DefaultGroovyMethods.asBoolean(ctx.VISIBILITY_MODIFIER())
                        ? parseVisibilityModifiers(ctx.VISIBILITY_MODIFIER())
                        : Opcodes.ACC_PUBLIC;

        ClassNode[] exceptions = parseThrowsClause(ctx.throwsClause());
        DefaultGroovyMethods.leftShift(instance.innerClassesDefinedInMethod, new ArrayList());
        final ConstructorNode constructorNode = classNode.addConstructor(modifiers, parseParameters(ctx.argumentDeclarationList()), exceptions, parseStatement(DefaultGroovyMethods.asType(ctx.blockStatement(), GroovyParser.BlockStatementContext.class)));
        DefaultGroovyMethods.each(instance.innerClassesDefinedInMethod.pop(), new Closure<ConstructorNode>(null, null) {
            public ConstructorNode doCall(InnerClassNode it) {
                it.setEnclosingMethod(constructorNode);
                return constructorNode;
            }

            public ConstructorNode doCall() {
                return doCall(null);
            }

        });
        setupNodeLocation(constructorNode, ctx);
        constructorNode.setSyntheticPublic(ctx.VISIBILITY_MODIFIER() == null);
        return constructorNode;
    }

    public static Statement parseStatement(GroovyParser.StatementContext ctx) {
        if (ctx instanceof GroovyParser.ForColonStatementContext)
            parseStatement((GroovyParser.ForColonStatementContext)ctx);
        if (ctx instanceof GroovyParser.IfStatementContext)
            return parseStatement((GroovyParser.IfStatementContext)ctx);
        if (ctx instanceof GroovyParser.NewArrayStatementContext)
            return parseStatement((GroovyParser.NewArrayStatementContext)ctx);
        if (ctx instanceof GroovyParser.TryCatchFinallyStatementContext)
            return parseStatement((GroovyParser.TryCatchFinallyStatementContext)ctx);
        if (ctx instanceof GroovyParser.ThrowStatementContext)
            return parseStatement((GroovyParser.ThrowStatementContext)ctx);
        if (ctx instanceof GroovyParser.ClassicForStatementContext)
            return parseStatement((GroovyParser.ClassicForStatementContext)ctx);
        if (ctx instanceof GroovyParser.DeclarationStatementContext)
            return parseStatement((GroovyParser.DeclarationStatementContext)ctx);
        if (ctx instanceof GroovyParser.ReturnStatementContext)
            return parseStatement((GroovyParser.ReturnStatementContext)ctx);
        if (ctx instanceof GroovyParser.ExpressionStatementContext)
            return parseStatement((GroovyParser.ExpressionStatementContext)ctx);
        if (ctx instanceof GroovyParser.ForInStatementContext)
            return parseStatement((GroovyParser.ForInStatementContext)ctx);
        if (ctx instanceof GroovyParser.ForColonStatementContext)
            return parseStatement((GroovyParser.ForColonStatementContext)ctx);
        if (ctx instanceof GroovyParser.SwitchStatementContext)
            return parseStatement((GroovyParser.SwitchStatementContext)ctx);
        if (ctx instanceof GroovyParser.WhileStatementContext)
            return parseStatement((GroovyParser.WhileStatementContext)ctx);
        if (ctx instanceof GroovyParser.ControlStatementContext)
            return parseStatement((GroovyParser.ControlStatementContext)ctx);
        if (ctx instanceof GroovyParser.CommandExpressionStatementContext)
            return parseStatement((GroovyParser.CommandExpressionStatementContext)ctx);
        if (ctx instanceof GroovyParser.NewInstanceStatementContext)
            return parseStatement((GroovyParser.NewInstanceStatementContext)ctx);

        throw new RuntimeException("Unsupported statement type! " + ctx.getText());
    }

    public static Statement parseStatement(GroovyParser.BlockStatementContext ctx) {
        final BlockStatement statement = new BlockStatement();
        if (!DefaultGroovyMethods.asBoolean(ctx)) return statement;

        DefaultGroovyMethods.each(ctx.statement(), new Closure<Object>(null, null) {
            public void doCall(GroovyParser.StatementContext it) {
                statement.addStatement(parseStatement(it));
            }

            public void doCall() {
                doCall(null);
            }

        });
        return setupNodeLocation(statement, ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ExpressionStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parseExpression(ctx.expression())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.IfStatementContext ctx) {
        Statement trueBranch = parse(ctx.statementBlock(0));
        Statement falseBranch = DefaultGroovyMethods.asBoolean(ctx.KW_ELSE())
                                ? parse(ctx.statementBlock(1))
                                : EmptyStatement.INSTANCE;
        BooleanExpression expression = new BooleanExpression(parseExpression(ctx.expression()));
        return setupNodeLocation(new IfStatement(expression, trueBranch, falseBranch), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.WhileStatementContext ctx) {
        return setupNodeLocation(new WhileStatement(new BooleanExpression(parseExpression(ctx.expression())), parse(ctx.statementBlock())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ClassicForStatementContext ctx) {
        ClosureListExpression expression = new ClosureListExpression();

        Boolean captureNext = false;
        for (ParseTree c : ctx.children) {
            // FIXME terrible logic.
            Boolean isSemicolon = c instanceof TerminalNode && (((TerminalNode)c).getSymbol().getText().equals(";") || ((TerminalNode)c).getSymbol().getText().equals("(") || ((TerminalNode)c).getSymbol().getText().equals(")"));
            if (captureNext && isSemicolon) expression.addExpression(EmptyExpression.INSTANCE);
            else if (captureNext && c instanceof GroovyParser.ExpressionContext)
                expression.addExpression(parseExpression((GroovyParser.ExpressionContext)c));
            captureNext = isSemicolon;
        }


        Parameter parameter = ForStatement.FOR_LOOP_DUMMY;
        return setupNodeLocation(new ForStatement(parameter, expression, parse(ctx.statementBlock())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ForInStatementContext ctx) {
        Parameter parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().getText());
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().getSymbol());

        return setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ForColonStatementContext ctx) {
        if (!DefaultGroovyMethods.asBoolean(ctx.typeDeclaration()))
            throw new RuntimeException("Classic for statement require type to be declared.");
        Parameter parameter = new Parameter(parseTypeDeclaration(ctx.typeDeclaration()), ctx.IDENTIFIER().getText());
        parameter = setupNodeLocation(parameter, ctx.IDENTIFIER().getSymbol());

        return setupNodeLocation(new ForStatement(parameter, parseExpression(ctx.expression()), parse(ctx.statementBlock())), ctx);
    }

    public static Statement parse(GroovyParser.StatementBlockContext ctx) {
        if (DefaultGroovyMethods.asBoolean(ctx.statement()))
            return setupNodeLocation(parseStatement(ctx.statement()), ctx.statement());
        else return parseStatement(ctx.blockStatement());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.SwitchStatementContext ctx) {
        List<CaseStatement> caseStatements = new ArrayList<CaseStatement>();
        for (GroovyParser.CaseStatementContext caseStmt : ctx.caseStatement()) {
            BlockStatement stmt = new BlockStatement();// #BSC
            for (GroovyParser.StatementContext st : caseStmt.statement()) stmt.addStatement(parseStatement(st));

            DefaultGroovyMethods.leftShift(caseStatements, setupNodeLocation(new CaseStatement(parseExpression(caseStmt.expression()), stmt), caseStmt.KW_CASE().getSymbol()));// There only 'case' kw was highlighted in parser old version.
        }


        Statement defaultStatement;
        if (DefaultGroovyMethods.asBoolean(ctx.KW_DEFAULT())) {
            defaultStatement = new BlockStatement();// #BSC
            for (GroovyParser.StatementContext stmt : ctx.statement())
                ((BlockStatement)defaultStatement).addStatement(parseStatement(stmt));
        } else defaultStatement = EmptyStatement.INSTANCE;// TODO Refactor empty stataements and expressions.

        return new SwitchStatement(parseExpression(ctx.expression()), caseStatements, defaultStatement);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.DeclarationStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parseDeclaration(ctx.declarationRule())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.NewArrayStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parse(ctx.newArrayRule())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.NewInstanceStatementContext ctx) {
        return setupNodeLocation(new ExpressionStatement(parse(ctx.newInstanceRule())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ControlStatementContext ctx) {
        // TODO check validity. Labeling support.
        // Fake inspection result should be suppressed.
        //noinspection GroovyConditionalWithIdenticalBranches
        return setupNodeLocation(DefaultGroovyMethods.asBoolean(ctx.KW_BREAK())
                                 ? new BreakStatement()
                                 : new ContinueStatement(), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ReturnStatementContext ctx) {
        GroovyParser.ExpressionContext expression = ctx.expression();
        return setupNodeLocation(new ReturnStatement(DefaultGroovyMethods.asBoolean(expression)
                                                     ? parseExpression(expression)
                                                     : EmptyExpression.INSTANCE), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.ThrowStatementContext ctx) {
        return setupNodeLocation(new ThrowStatement(parseExpression(ctx.expression())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.TryCatchFinallyStatementContext ctx) {
        Object finallyStatement;

        GroovyParser.BlockStatementContext finallyBlockStatement = ctx.finallyBlock() != null ? ctx.finallyBlock().blockStatement() : null;
        if (finallyBlockStatement != null) {
            BlockStatement fbs = new BlockStatement();
            fbs.addStatement(parseStatement(finallyBlockStatement));
            finallyStatement = setupNodeLocation(fbs, finallyBlockStatement);

        } else finallyStatement = EmptyStatement.INSTANCE;

        final TryCatchStatement statement = new TryCatchStatement(parseStatement(DefaultGroovyMethods.asType(ctx.tryBlock().blockStatement(), GroovyParser.BlockStatementContext.class)), (Statement)finallyStatement);
        DefaultGroovyMethods.each(ctx.catchBlock(), new Closure<List<GroovyParser.ClassNameExpressionContext>>(null, null) {
            public List<GroovyParser.ClassNameExpressionContext> doCall(GroovyParser.CatchBlockContext it) {
                final Statement catchBlock = parseStatement(DefaultGroovyMethods.asType(it.blockStatement(), GroovyParser.BlockStatementContext.class));
                final String var = it.IDENTIFIER().getText();

                List<GroovyParser.ClassNameExpressionContext> classNameExpression = it.classNameExpression();
                if (!DefaultGroovyMethods.asBoolean(classNameExpression))
                    statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(ClassHelper.OBJECT_TYPE, var), catchBlock), it));
                else {
                    DefaultGroovyMethods.each(classNameExpression, new Closure<Object>(null, null) {
                        public void doCall(GroovyParser.ClassNameExpressionContext it) {
                            statement.addCatch(setupNodeLocation(new CatchStatement(new Parameter(parseExpression(DefaultGroovyMethods.asType(it, GroovyParser.ClassNameExpressionContext.class)), var), catchBlock), it));
                        }

                        public void doCall() {
                            doCall(null);
                        }
                    });
                }
                return null;
            }

            public List<GroovyParser.ClassNameExpressionContext> doCall() {
                return doCall(null);
            }

        });
        return statement;
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Statement parseStatement(GroovyParser.CommandExpressionStatementContext ctx) {
        Expression expression = null;
        List<List<ParseTree>> list = DefaultGroovyMethods.collate(ctx.cmdExpressionRule().children, 2);
        for (List<ParseTree> c : list) {
            final Iterator<ParseTree> iterator = c.iterator();
            ParseTree c1 = iterator.hasNext() ? iterator.next() : null;
            ParseTree c0 = iterator.hasNext() ? iterator.next() : null;

            if (c.size() == 1) expression = new PropertyExpression(expression, c1.getText());
            else {
                assert c0 instanceof GroovyParser.ArgumentListContext;
                if (c1 instanceof TerminalNode) {
                    expression = new MethodCallExpression(expression, ((TerminalNode)c1).getText(), createArgumentList((GroovyParser.ArgumentListContext)c0));
                    ((MethodCallExpression)expression).setImplicitThis(false);
                } else if (c1 instanceof GroovyParser.PathExpressionContext) {
                    String methodName;
                    boolean implicitThis;
                    ArrayList<Object> objects = parsePathExpression((GroovyParser.PathExpressionContext)c1);
                    expression = (Expression)objects.get(0);
                    methodName = (String)objects.get(1);
                    implicitThis = (Boolean)objects.get(2);

                    expression = new MethodCallExpression(expression, methodName, createArgumentList((GroovyParser.ArgumentListContext)c0));
                    ((MethodCallExpression)expression).setImplicitThis(implicitThis);
                }

            }

        }


        return new ExpressionStatement(expression);
    }

    /**
     * Parse path expression.
     *
     * @param ctx
     * @return tuple of 3 values: Expression, String methodName and boolean implicitThis flag.
     */
    public static ArrayList<Object> parsePathExpression(GroovyParser.PathExpressionContext ctx) {
        Expression expression;
        List<TerminalNode> identifiers = ctx.IDENTIFIER();
        switch (identifiers.size()) {
        case 1:
            expression = VariableExpression.THIS_EXPRESSION;
            break;
        case 2:
            expression = new VariableExpression(identifiers.get(0).getText());
            break;
        default:
            expression = DefaultGroovyMethods.inject(identifiers.subList(1, identifiers.size() - 1), new VariableExpression(identifiers.get(0).getText()), new Closure<PropertyExpression>(null, null) {
                public PropertyExpression doCall(Expression expr, Object prop) {
                    return new PropertyExpression(expr, ((TerminalNode)prop).getText());
                }

            });
            log.info(expression.getText());
            break;
        }
        return new ArrayList<Object>(Arrays.asList(expression, DefaultGroovyMethods.last(identifiers).getSymbol().getText(), identifiers.size() == 1));
    }

    public static Expression parseExpression(GroovyParser.ExpressionContext ctx) {
        if (ctx instanceof GroovyParser.ParenthesisExpressionContext)
            return parseExpression((GroovyParser.ParenthesisExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantIntegerExpressionContext)
            return parseExpression((GroovyParser.ConstantIntegerExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.PostfixExpressionContext)
            return parseExpression((GroovyParser.PostfixExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ClosureExpressionContext)
            return parseExpression((GroovyParser.ClosureExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.AssignmentExpressionContext)
            return parseExpression((GroovyParser.AssignmentExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantDecimalExpressionContext)
            return parseExpression((GroovyParser.ConstantDecimalExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.TernaryExpressionContext)
            return parseExpression((GroovyParser.TernaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.MethodCallExpressionContext)
            return parseExpression((GroovyParser.MethodCallExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.DeclarationExpressionContext)
            return parseExpression((GroovyParser.DeclarationExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ElvisExpressionContext)
            return parseExpression((GroovyParser.ElvisExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.BinaryExpressionContext)
            return parseExpression((GroovyParser.BinaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NullExpressionContext)
            return parseExpression((GroovyParser.NullExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ListConstructorContext)
            return parseExpression((GroovyParser.ListConstructorContext)ctx);
        else if (ctx instanceof GroovyParser.PrefixExpressionContext)
            return parseExpression((GroovyParser.PrefixExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.ConstantExpressionContext)
            return parseExpression((GroovyParser.ConstantExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NewArrayExpressionContext)
            return parseExpression((GroovyParser.NewArrayExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.FieldAccessExpressionContext)
            return parseExpression((GroovyParser.FieldAccessExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.VariableExpressionContext)
            return parseExpression((GroovyParser.VariableExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.NewInstanceExpressionContext)
            return parseExpression((GroovyParser.NewInstanceExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.BoolExpressionContext)
            return parseExpression((GroovyParser.BoolExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.CallExpressionContext)
            return parseExpression((GroovyParser.CallExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.UnaryExpressionContext)
            return parseExpression((GroovyParser.UnaryExpressionContext)ctx);
        else if (ctx instanceof GroovyParser.MapConstructorContext)
            return parseExpression((GroovyParser.MapConstructorContext)ctx);
        else if (ctx instanceof GroovyParser.GstringExpressionContext)
            return parseExpression((GroovyParser.GstringExpressionContext)ctx);

        throw new RuntimeException("Unsupported expression type! " + String.valueOf(ctx));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.NewArrayExpressionContext ctx) {
        return parse(ctx.newArrayRule());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.NewInstanceExpressionContext ctx) {
        return parse(ctx.newInstanceRule());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.ParenthesisExpressionContext ctx) {
        return parseExpression(ctx.expression());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.ListConstructorContext ctx) {
        ListExpression expression = new ListExpression(DefaultGroovyMethods.collect(ctx.expression(), new MethodClosure(ASTBuilder.class, "parseExpression")));
        return setupNodeLocation(expression, ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.MapConstructorContext ctx) {
        final List collect = DefaultGroovyMethods.collect(ctx.mapEntry(), new MethodClosure(ASTBuilder.class, "parseExpression"));
        return setupNodeLocation(new MapExpression(DefaultGroovyMethods.asBoolean(collect)
                                                   ? collect
                                                   : new ArrayList()), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static MapEntryExpression parseExpression(GroovyParser.MapEntryContext ctx) {
        Expression keyExpr;
        Expression valueExpr;
        List<GroovyParser.ExpressionContext> expressions = ctx.expression();
        if (expressions.size() == 1) {
            keyExpr = DefaultGroovyMethods.asBoolean(ctx.gstring())
                      ? parseExpression(ctx.gstring())
                      : new ConstantExpression(DefaultGroovyMethods.asBoolean(ctx.IDENTIFIER())
                                               ? ctx.IDENTIFIER().getText()
                                               : parseString(ctx.STRING()));

            valueExpr = parseExpression(expressions.get(0));
        } else {
            keyExpr = parseExpression(expressions.get(0));
            valueExpr = parseExpression(expressions.get(1));
        }

        return setupNodeLocation(new MapEntryExpression(keyExpr, valueExpr), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.ClosureExpressionContext ctx) {
        return parseExpression(ctx.closureExpressionRule());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.ClosureExpressionRuleContext ctx) {
        final Parameter[] parameters1 = parseParameters(ctx.argumentDeclarationList());
        Parameter[] parameters = DefaultGroovyMethods.asBoolean(ctx.argumentDeclarationList()) ? (
            DefaultGroovyMethods.asBoolean(parameters1)
            ? parameters1
            : null) : (new Parameter[0]);

        Statement statement = parseStatement(DefaultGroovyMethods.asType(ctx.blockStatement(), GroovyParser.BlockStatementContext.class));
        return setupNodeLocation(new ClosureExpression(parameters, statement), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.BinaryExpressionContext ctx) {
        TerminalNode c = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        int i = 1;
        for (ParseTree next = ctx.getChild(i + 1); next instanceof TerminalNode && ((TerminalNode)next).getSymbol().getType() == GroovyParser.GT; next = ctx.getChild(i + 1))
            i++;
        org.codehaus.groovy.syntax.Token op = createToken(c, i);
        Object expression;
        Expression left = parseExpression(ctx.expression(0));
        Expression right = null;// Will be initialized later, in switch. We should handle as and instanceof creating
        // ClassExpression for given IDENTIFIERS. So, switch should fall through.
        //noinspection GroovyFallthrough
        switch (op.getType()) {
        case Types.RANGE_OPERATOR:
            right = parseExpression(ctx.expression(1));
            expression = new RangeExpression(left, right, !op.getText().endsWith("<"));
            break;
        case Types.KEYWORD_AS:
            ClassNode classNode = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
            expression = CastExpression.asExpression(classNode, left);
            break;
        case Types.KEYWORD_INSTANCEOF:
            ClassNode rhClass = setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx.genericClassNameExpression());
            right = new ClassExpression(rhClass);
        default:
            if (!DefaultGroovyMethods.asBoolean(right)) right = parseExpression(ctx.expression(1));
            expression = new BinaryExpression(left, op, right);
            break;
        }

        ((Expression)expression).setColumnNumber(op.getStartColumn());
        ((Expression)expression).setLastColumnNumber(op.getStartColumn() + op.getText().length());
        ((Expression)expression).setLineNumber(op.getStartLine());
        ((Expression)expression).setLastLineNumber(op.getStartLine());
        return ((Expression)(expression));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.TernaryExpressionContext ctx) {
        BooleanExpression boolExpr = new BooleanExpression(parseExpression(ctx.expression(0)));
        Expression trueExpr = parseExpression(ctx.expression(1));
        Expression falseExpr = parseExpression(ctx.expression(2));
        return setupNodeLocation(new TernaryExpression(boolExpr, trueExpr, falseExpr), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.ElvisExpressionContext ctx) {
        Expression baseExpr = parseExpression(ctx.expression(0));
        Expression falseExpr = parseExpression(ctx.expression(1));
        return setupNodeLocation(new ElvisOperatorExpression(baseExpr, falseExpr), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.UnaryExpressionContext ctx) {
        Object node = null;
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class);
        if (DefaultGroovyMethods.isCase("-", op.getText())) {
            node = new UnaryMinusExpression(parseExpression(ctx.expression()));
        } else if (DefaultGroovyMethods.isCase("+", op.getText())) {
            node = new UnaryPlusExpression(parseExpression(ctx.expression()));
        } else if (DefaultGroovyMethods.isCase("!", op.getText())) {
            node = new NotExpression(parseExpression(ctx.expression()));
        } else if (DefaultGroovyMethods.isCase("~", op.getText())) {
            node = new BitwiseNegationExpression(parseExpression(ctx.expression()));
        } else {
            assert false : "There is no " + op.getText() + " handler.";
        }

        ((Expression)node).setColumnNumber(op.getSymbol().getCharPositionInLine() + 1);
        ((Expression)node).setLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastLineNumber(op.getSymbol().getLine());
        ((Expression)node).setLastColumnNumber(op.getSymbol().getCharPositionInLine() + 1 + op.getText().length());
        return ((Expression)(node));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.AnnotationParameterContext ctx) {
        if (ctx instanceof GroovyParser.AnnotationParamArrayExpressionContext) {
            GroovyParser.AnnotationParamArrayExpressionContext c = DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamArrayExpressionContext.class);
            return setupNodeLocation(new ListExpression(DefaultGroovyMethods.collect(c.annotationParameter(), new Closure<Expression>(null, null) {
                public Expression doCall(GroovyParser.AnnotationParameterContext it) {return parseExpression(it);}

                public Expression doCall() {
                    return doCall(null);
                }

            })), c);
        } else if (ctx instanceof GroovyParser.AnnotationParamBoolExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamBoolExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamClassExpressionContext) {
            return setupNodeLocation(new ClassExpression(parseExpression((DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamClassExpressionContext.class)).genericClassNameExpression())), ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamDecimalExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamDecimalExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamIntegerExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamIntegerExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamNullExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamNullExpressionContext)ctx);
        } else if (ctx instanceof GroovyParser.AnnotationParamPathExpressionContext) {
            GroovyParser.AnnotationParamPathExpressionContext c = DefaultGroovyMethods.asType(ctx, GroovyParser.AnnotationParamPathExpressionContext.class);
            return collectPathExpression(c.pathExpression());
        } else if (ctx instanceof GroovyParser.AnnotationParamStringExpressionContext) {
            return parseExpression((GroovyParser.AnnotationParamStringExpressionContext)ctx);
        }
        throw new CompilationFailedException(CompilePhase.PARSING.getPhaseNumber(), instance.sourceUnit, new IllegalStateException(String.valueOf(ctx) + " is prohibited inside annotations."));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.VariableExpressionContext ctx) {
        return setupNodeLocation(new VariableExpression(ctx.IDENTIFIER().getText()), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.FieldAccessExpressionContext ctx) {
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        String text = ctx.IDENTIFIER().getText();
        Expression left = parseExpression(ctx.expression());
        ConstantExpression right = new ConstantExpression(text);
        Object node;
        if (op.getText().equals(".@"))
            node = new AttributeExpression(left, right);
        else {
            node = new PropertyExpression(left, right, new ArrayList<String>(Arrays.asList("?.", "*.")).contains(ctx.getChild(1).getText()));
        }

        setupNodeLocation((PropertyExpression)node, ctx);
        ((PropertyExpression)node).setSpreadSafe(op.getText().equals("*."));
        return ((Expression)(node));
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static PrefixExpression parseExpression(GroovyParser.PrefixExpressionContext ctx) {
        return setupNodeLocation(new PrefixExpression(createToken(DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class)), parseExpression(ctx.expression())), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static PostfixExpression parseExpression(GroovyParser.PostfixExpressionContext ctx) {
        return setupNodeLocation(new PostfixExpression(parseExpression(ctx.expression()), createToken(DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class))), ctx);
    }

    public static ConstantExpression parseDecimal(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseDecimal(text), !text.startsWith("-")), ctx);// Why 10 is int but -10 is Integer?
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.AnnotationParamDecimalExpressionContext ctx) {
        return parseDecimal(ctx.DECIMAL().getText(), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.ConstantDecimalExpressionContext ctx) {
        return parseDecimal(ctx.DECIMAL().getText(), ctx);
    }

    public static ConstantExpression parseInteger(String text, ParserRuleContext ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
    }

    public static ConstantExpression parseInteger(String text, Token ctx) {
        return setupNodeLocation(new ConstantExpression(Numbers.parseInteger(text), !text.startsWith("-")), ctx);//Why 10 is int but -10 is Integer?
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.ConstantIntegerExpressionContext ctx) {
        return parseInteger(ctx.INTEGER().getText(), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.AnnotationParamIntegerExpressionContext ctx) {
        return parseInteger(ctx.INTEGER().getText(), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.BoolExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(!DefaultGroovyMethods.asBoolean(ctx.KW_FALSE()), true), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.AnnotationParamBoolExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(!DefaultGroovyMethods.asBoolean(ctx.KW_FALSE()), true), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseConstantString(ParserRuleContext ctx) {
        String text = ctx.getText();
        Boolean isSlashy = text.startsWith("/");

        if (text.startsWith("'''") || text.startsWith("\"\"\""))
            text = text.length() == 6 ? "" : text.substring(3, text.length() - 3);
        else if (text.startsWith("'") || text.startsWith("/") || text.startsWith("\""))
            text = text.length() == 2 ? "" : text.substring(1, text.length() - 1);

        //Find escapes.
        if (!isSlashy)
            text = StringUtil.replaceStandardEscapes(StringUtil.replaceOctalEscapes(text));
        else
            text = text.replace("\\/", "/");

        return setupNodeLocation(new ConstantExpression(text, true), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.ConstantExpressionContext ctx) {
        return parseConstantString(ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static ConstantExpression parseExpression(GroovyParser.AnnotationParamStringExpressionContext ctx) {
        return parseConstantString(ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.GstringExpressionContext ctx) {
        return parseExpression(ctx.gstring());
    }

    public static Expression parseExpression(GroovyParser.GstringContext ctx) {
        Closure<String> clearStart = new Closure<String>(null, null) {
            public String doCall(String it) {
                return it.length() == 2
                       ? ""
                       : DefaultGroovyMethods.getAt(it, new IntRange(true, 1, -2));
            }

        };
        final Closure<String> clearPart = new Closure<String>(null, null) {
            public String doCall(String it) {
                return it.length() == 1
                       ? ""
                       : DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -2));
            }

        };
        Closure<String> clearEnd = new Closure<String>(null, null) {
            public String doCall(String it) {
                return it.length() == 1
                       ? ""
                       : DefaultGroovyMethods.getAt(it, new IntRange(true, 0, -2));
            }

        };
        Collection<String> strings = DefaultGroovyMethods.plus(DefaultGroovyMethods.plus(new ArrayList<String>(Arrays.asList(clearStart.call(ctx.GSTRING_START().getText()))), DefaultGroovyMethods.collect(ctx.GSTRING_PART(), new Closure<String>(null, null) {
            public String doCall(TerminalNode it) {return clearPart.call(it.getText());}

            public String doCall() {
                return doCall(null);
            }

        })), new ArrayList<String>(Arrays.asList(clearEnd.call(ctx.GSTRING_END().getText()))));
        final ArrayList expressions = new ArrayList();

        final List<ParseTree> children = ctx.children;
        DefaultGroovyMethods.eachWithIndex(children, new Closure<Collection>(null, null) {
            public Collection doCall(Object it, Integer i) {
                if (it instanceof GroovyParser.ExpressionContext) {
                    // We can guarantee, that it will be at least fallback ExpressionContext multimethod overloading, that can handle such situation.
                    //noinspection GroovyAssignabilityCheck
                    return DefaultGroovyMethods.leftShift(expressions, (DefaultGroovyMethods.asType(parseExpression((GroovyParser.ExpressionContext)it), Expression.class)));
                } else if (it instanceof GroovyParser.GstringPathExpressionContext)
                    return DefaultGroovyMethods.leftShift(expressions, collectPathExpression((GroovyParser.GstringPathExpressionContext)it));
                else if (it instanceof TerminalNode) {
                    ParseTree next = i + 1 < children.size() ? children.get(i + 1) : null;
                    if (next instanceof TerminalNode && (DefaultGroovyMethods.asType(next, TerminalNode.class)).getSymbol().getType() == GroovyParser.RCURVE)
                        return DefaultGroovyMethods.leftShift(expressions, new ConstantExpression(null));
                }
                return null;
            }

        });
        GStringExpression gstringNode = new GStringExpression(ctx.getText(), DefaultGroovyMethods.collect(strings, new Closure<ConstantExpression>(null, null) {
            public ConstantExpression doCall(String it) {return new ConstantExpression(it);}

            public ConstantExpression doCall() {
                return doCall(null);
            }

        }), expressions);
        return setupNodeLocation(gstringNode, ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.NullExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(null), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.AnnotationParamNullExpressionContext ctx) {
        return setupNodeLocation(new ConstantExpression(null), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.AssignmentExpressionContext ctx) {
        Expression left = parseExpression(ctx.expression(0));// TODO reference to AntlrParserPlugin line 2304 for error handling.
        Expression right = parseExpression(ctx.expression(1));
        return setupNodeLocation(new BinaryExpression(left, createToken(DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class)), right), ctx);
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.DeclarationExpressionContext ctx) {
        return parseDeclaration(ctx.declarationRule());
    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static Expression parseExpression(GroovyParser.CallExpressionContext ctx) {

        Object methodNode;
        //FIXME in log a, b; a is treated as path expression and became a method call instead of variable
        if (!DefaultGroovyMethods.asBoolean(ctx.LPAREN()) && ctx.closureExpressionRule().size() == 0)
            return collectPathExpression(ctx.pathExpression());

        // Collect closure's in argumentList expression.
        final Expression argumentListExpression = createArgumentList(ctx.argumentList());
        DefaultGroovyMethods.each(ctx.closureExpressionRule(), new Closure<Object>(null, null) {
            public Object doCall(GroovyParser.ClosureExpressionRuleContext it) {return DefaultGroovyMethods.invokeMethod(argumentListExpression, "addExpression", new Object[]{ ASTBuilder.parseExpression(it) });}

            public Object doCall() {
                return doCall(null);
            }

        });

        //noinspection GroovyAssignabilityCheck
        List<Object> iterator = parsePathExpression(ctx.pathExpression());
        Expression expression = (Expression)iterator.get(0);
        String methodName = (String)iterator.get(1);
        boolean implicitThis = (Boolean)iterator.get(2);

        methodNode = new MethodCallExpression(expression, methodName, argumentListExpression);
        ((MethodCallExpression)methodNode).setImplicitThis(implicitThis);
        return (Expression)methodNode;
    }

    public static Expression collectPathExpression(GroovyParser.PathExpressionContext ctx) {
        List<TerminalNode> identifiers = ctx.IDENTIFIER();
        switch (identifiers.size()) {
        case 1:
            return new VariableExpression(identifiers.get(0).getText());
        default:
            Expression inject = DefaultGroovyMethods.inject(identifiers.subList(1, identifiers.size()), new VariableExpression(identifiers.get(0).getText()), new Closure<PropertyExpression>(null, null) {
                public PropertyExpression doCall(Object val, Object prop) {
                    return new PropertyExpression(DefaultGroovyMethods.asType(val, Expression.class), new ConstantExpression(((TerminalNode)prop).getText()));
                }

            });
            return inject;
        }
    }

    public static Expression collectPathExpression(GroovyParser.GstringPathExpressionContext ctx) {
        if (!DefaultGroovyMethods.asBoolean(ctx.GSTRING_PATH_PART()))
            return new VariableExpression(ctx.IDENTIFIER().getText());
        else {
            Expression inj = DefaultGroovyMethods.inject(ctx.GSTRING_PATH_PART(), new VariableExpression(ctx.IDENTIFIER().getText()), new Closure<PropertyExpression>(null, null) {
                public PropertyExpression doCall(Object val, Object prop) {
                    return new PropertyExpression(DefaultGroovyMethods.asType(val, Expression.class), new ConstantExpression(DefaultGroovyMethods.getAt(((TerminalNode)prop).getText(), new IntRange(true, 1, -1))));
                }

            });
            return inj;
        }

    }

    @SuppressWarnings("GroovyUnusedDeclaration") public static MethodCallExpression parseExpression(GroovyParser.MethodCallExpressionContext ctx) {
        ConstantExpression method = new ConstantExpression(ctx.IDENTIFIER().getText());
        Expression argumentListExpression = createArgumentList(ctx.argumentList());
        MethodCallExpression expression = new MethodCallExpression(parseExpression(ctx.expression()), method, argumentListExpression);
        expression.setImplicitThis(false);
        TerminalNode op = DefaultGroovyMethods.asType(ctx.getChild(1), TerminalNode.class);
        expression.setSpreadSafe(op.getText().equals("*."));
        expression.setSafe(op.getText().equals("?."));
        return expression;
    }

    public static ClassNode parseExpression(GroovyParser.ClassNameExpressionContext ctx) {
        return setupNodeLocation(ClassHelper.make(DefaultGroovyMethods.join(ctx.IDENTIFIER(), ".")), ctx);
    }

    public static ClassNode parseExpression(GroovyParser.GenericClassNameExpressionContext ctx) {
        ClassNode classNode = parseExpression(ctx.classNameExpression());

        if (DefaultGroovyMethods.asBoolean(ctx.LBRACK())) classNode = classNode.makeArray();
        classNode.setGenericsTypes(parseGenericList(ctx.genericList()));
        return setupNodeLocation(classNode, ctx);
    }

    public static GenericsType[] parseGenericList(GroovyParser.GenericListContext ctx) {
        if (ctx == null)
            return null;
        List<GenericsType> collect = DefaultGroovyMethods.collect(ctx.genericListElement(), new Closure<GenericsType>(null, null) {
            public GenericsType doCall(GroovyParser.GenericListElementContext it) {
                if (it instanceof GroovyParser.GenericsConcreteElementContext)
                    return setupNodeLocation(new GenericsType(parseExpression(((GroovyParser.GenericsConcreteElementContext)it).genericClassNameExpression())), it);
                else {
                    assert it instanceof GroovyParser.GenericsWildcardElementContext;
                    GroovyParser.GenericsWildcardElementContext gwec = (GroovyParser.GenericsWildcardElementContext)it;
                    ClassNode baseType = ClassHelper.makeWithoutCaching("?");
                    ClassNode[] upperBounds = null;
                    ClassNode lowerBound = null;
                    if (DefaultGroovyMethods.asBoolean(gwec.KW_EXTENDS())) {
                        ClassNode classNode = parseExpression(gwec.genericClassNameExpression());
                        upperBounds = new ClassNode[]{ classNode };
                    } else if (DefaultGroovyMethods.asBoolean(gwec.KW_SUPER()))
                        lowerBound = parseExpression(gwec.genericClassNameExpression());

                    GenericsType type = new GenericsType(baseType, upperBounds, lowerBound);
                    type.setWildcard(true);
                    type.setName("?");
                    return setupNodeLocation(type, it);
                }

            }

            public GenericsType doCall() {
                return doCall(null);
            }

        });
        return collect.toArray(new GenericsType[collect.size()]);
    }

    public static GenericsType[] parseGenericDeclaration(GroovyParser.GenericDeclarationListContext ctx) {
        if (ctx == null)
            return null;
        List<GenericsType> genericTypes = DefaultGroovyMethods.collect(ctx.genericsDeclarationElement(), new Closure<GenericsType>(null, null) {
            public GenericsType doCall(GroovyParser.GenericsDeclarationElementContext it) {
                ClassNode classNode = parseExpression(it.genericClassNameExpression(0));
                ClassNode[] upperBounds = null;
                if (DefaultGroovyMethods.asBoolean(it.KW_EXTENDS())) {
                    List<GroovyParser.GenericClassNameExpressionContext> genericClassNameExpressionContexts = DefaultGroovyMethods.toList(it.genericClassNameExpression());
                    upperBounds = DefaultGroovyMethods.asType(DefaultGroovyMethods.collect(genericClassNameExpressionContexts.subList(1, genericClassNameExpressionContexts.size()), new MethodClosure(ASTBuilder.class, "parseExpression")), ClassNode[].class);
                }
                GenericsType type = new GenericsType(classNode, upperBounds, null);
                return setupNodeLocation(type, it);
            }

            public GenericsType doCall() {
                return doCall(null);
            }

        });
        return  genericTypes.toArray(new GenericsType[genericTypes.size()]);
    }

    public static Expression parseDeclaration(GroovyParser.DeclarationRuleContext ctx) {
        VariableExpression left = new VariableExpression(ctx.IDENTIFIER().getText(), parseTypeDeclaration(ctx.typeDeclaration()));
        Integer col = ctx.getStart().getCharPositionInLine() + 1;// FIXME Why assignment token location is it's first occurrence.
        org.codehaus.groovy.syntax.Token token = new org.codehaus.groovy.syntax.Token(Types.ASSIGN, "=", ctx.getStart().getLine(), col);
        Expression right = ctx.getChildCount() == 2 ? new EmptyExpression() : parseExpression(ctx.expression());

        DeclarationExpression expression = new DeclarationExpression(left, token, right);
        attachAnnotations(expression, ctx.annotationClause());
        return setupNodeLocation(expression, ctx);
    }

    @SuppressWarnings("UnnecessaryQualifiedReference") private static Expression createArgumentList(GroovyParser.ArgumentListContext ctx) {
        final List<MapEntryExpression> mapArgs = new ArrayList<MapEntryExpression>();
        final List<Expression> expressions = new ArrayList<Expression>();
        DefaultGroovyMethods.each((ctx == null
                                   ? null
                                   : ctx.children), new Closure<Collection<? extends Expression>>(null, null) {
            public Collection<? extends Expression> doCall(ParseTree it) {
                if (it instanceof GroovyParser.ArgumentContext) {
                    if (DefaultGroovyMethods.asBoolean(((GroovyParser.ArgumentContext)it).mapEntry()))
                        return DefaultGroovyMethods.leftShift(mapArgs, parseExpression(((GroovyParser.ArgumentContext)it).mapEntry()));
                    else
                        return DefaultGroovyMethods.leftShift(expressions, parseExpression(((GroovyParser.ArgumentContext)it).expression()));
                } else if (it instanceof GroovyParser.ClosureExpressionRuleContext)
                    return DefaultGroovyMethods.leftShift(expressions, parseExpression((GroovyParser.ClosureExpressionRuleContext)it));
                return null;
            }

            public Collection<? extends Expression> doCall() {
                return doCall(null);
            }

        });
        if (DefaultGroovyMethods.asBoolean(expressions)) {
            if (DefaultGroovyMethods.asBoolean(mapArgs))
                expressions.add(0, new MapExpression(mapArgs));
            return new ArgumentListExpression(expressions);
        } else {
            if (DefaultGroovyMethods.asBoolean(mapArgs))
                return new TupleExpression(new NamedArgumentListExpression(mapArgs));
            else return new ArgumentListExpression();
        }

    }

    public static void attachAnnotations(AnnotatedNode node, List<GroovyParser.AnnotationClauseContext> ctxs) {
        for (GroovyParser.AnnotationClauseContext ctx : ctxs) {
            AnnotationNode annotation = parseAnnotation(ctx);
            node.addAnnotation(annotation);
        }

    }

    public static List<AnnotationNode> parseAnnotations(List<GroovyParser.AnnotationClauseContext> ctxs) {
        return DefaultGroovyMethods.collect(ctxs, new Closure<AnnotationNode>(null, null) {
            public AnnotationNode doCall(GroovyParser.AnnotationClauseContext it) {return parseAnnotation(it);}

            public AnnotationNode doCall() {
                return doCall(null);
            }

        });
    }

    public static AnnotationNode parseAnnotation(GroovyParser.AnnotationClauseContext ctx) {
        AnnotationNode node = new AnnotationNode(parseExpression(ctx.genericClassNameExpression()));
        if (DefaultGroovyMethods.asBoolean(ctx.annotationElement()))
            node.addMember("value", parseAnnotationElement(ctx.annotationElement()));
        else {
            for (GroovyParser.AnnotationElementPairContext pair : ctx.annotationElementPair()) {
                node.addMember(pair.IDENTIFIER().getText(), parseAnnotationElement(pair.annotationElement()));
            }

        }


        return setupNodeLocation(node, ctx);
    }

    public static Expression parseAnnotationElement(GroovyParser.AnnotationElementContext ctx) {
        GroovyParser.AnnotationClauseContext annotationClause = ctx.annotationClause();
        if (DefaultGroovyMethods.asBoolean(annotationClause))
            return setupNodeLocation(new AnnotationConstantExpression(parseAnnotation(annotationClause)), annotationClause);
        else return parseExpression(ctx.annotationParameter());
    }

    public static ClassNode[] parseThrowsClause(GroovyParser.ThrowsClauseContext ctx) {
        List list = DefaultGroovyMethods.asBoolean(ctx)
                    ? DefaultGroovyMethods.collect(ctx.classNameExpression(), new Closure<ClassNode>(null, null) {
            public ClassNode doCall(GroovyParser.ClassNameExpressionContext it) {return parseExpression(it);}

            public ClassNode doCall() {
                return doCall(null);
            }

        })
                    : new ArrayList();
        return (ClassNode[])list.toArray(new ClassNode[list.size()]);
    }

    /**
     * @param node
     * @param cardinality Used for handling GT ">" operator, which can be repeated to give bitwise shifts >> or >>>
     * @return
     */
    public static org.codehaus.groovy.syntax.Token createToken(TerminalNode node, int cardinality) {
        String text = DefaultGroovyMethods.multiply(node.getText(), cardinality);
        return new org.codehaus.groovy.syntax.Token(node.getText().equals("..<") || node.getText().equals("..")
                                                    ? Types.RANGE_OPERATOR
                                                    : Types.lookup(text, Types.ANY), text, node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine() + 1);
    }

    /**
     * @param node
     * @param cardinality Used for handling GT ">" operator, which can be repeated to give bitwise shifts >> or >>>
     * @return
     */
    public static org.codehaus.groovy.syntax.Token createToken(TerminalNode node) {
        return ASTBuilder.createToken(node, 1);
    }

    public static ClassNode parseTypeDeclaration(GroovyParser.TypeDeclarationContext ctx) {
        return !DefaultGroovyMethods.asBoolean(ctx) || ctx.KW_DEF() != null
               ? ClassHelper.OBJECT_TYPE
               : setupNodeLocation(parseExpression(ctx.genericClassNameExpression()), ctx);
    }

    public static ArrayExpression parse(GroovyParser.NewArrayRuleContext ctx) {
        List<Expression> collect = DefaultGroovyMethods.collect(ctx.INTEGER(), new Closure<Expression>(null, null) {
            public Expression doCall(TerminalNode it) {return parseInteger(it.getText(), it.getSymbol());}

            public Expression doCall() {
                return doCall(null);
            }

        });
        ArrayExpression expression = new ArrayExpression(parseExpression(ctx.classNameExpression()), new ArrayList<Expression>(), collect);
        return setupNodeLocation(expression, ctx);
    }

    public static ConstructorCallExpression parse(GroovyParser.NewInstanceRuleContext ctx) {
        ClassNode creatingClass = DefaultGroovyMethods.asBoolean(ctx.genericClassNameExpression())
                                  ? parseExpression(ctx.genericClassNameExpression())
                                  : parseExpression(ctx.classNameExpression());
        if (DefaultGroovyMethods.asBoolean(ctx.LT())) creatingClass.setGenericsTypes(new GenericsType[0]);

        ConstructorCallExpression expression;
        if (!DefaultGroovyMethods.asBoolean(ctx.classBody())) {
            expression = setupNodeLocation(new ConstructorCallExpression(creatingClass, createArgumentList(ctx.argumentList())), ctx);
        } else {
            ClassNode outer = instance.classes.peek();
            InnerClassNode classNode = new InnerClassNode(outer, outer.getName() + "$" + String.valueOf((instance.anonymousClassesCount = ++instance.anonymousClassesCount)), Opcodes.ACC_PUBLIC, ClassHelper.make(creatingClass.getName()));
            expression = setupNodeLocation(new ConstructorCallExpression(classNode, createArgumentList(ctx.argumentList())), ctx);
            expression.setUsingAnonymousInnerClass(true);
            classNode.setAnonymous(true);
            DefaultGroovyMethods.leftShift(DefaultGroovyMethods.last(instance.innerClassesDefinedInMethod), classNode);
            instance.moduleNode.addClass(classNode);
            DefaultGroovyMethods.leftShift(instance.classes, classNode);
            instance.parseMembers(classNode, ctx.classBody().classMember());
            instance.classes.pop();
        }

        return expression;
    }

    public static Parameter[] parseParameters(GroovyParser.ArgumentDeclarationListContext ctx) {
        List<Parameter> parameterList = ctx == null || ctx.argumentDeclaration() == null ?
            new ArrayList<Parameter>(0) :
            DefaultGroovyMethods.collect(ctx.argumentDeclaration(), new Closure<Parameter>(null, null) {
                public Parameter doCall(GroovyParser.ArgumentDeclarationContext it) {
                    Parameter parameter = new Parameter(parseTypeDeclaration(it.typeDeclaration()), it.IDENTIFIER().getText());
                    attachAnnotations(parameter, it.annotationClause());
                    if (DefaultGroovyMethods.asBoolean(it.expression()))
                        parameter.setInitialExpression(parseExpression(it.expression()));
                    return setupNodeLocation(parameter, it);
                }

                public Parameter doCall() {
                    return doCall(null);
                }

            });
        return parameterList.toArray(new Parameter[parameterList.size()]);
    }

    public static MethodNode getOrCreateClinitMethod(ClassNode classNode) {
        MethodNode methodNode = DefaultGroovyMethods.find(classNode.getMethods(), new Closure<Boolean>(null, null) {
            public Boolean doCall(MethodNode it) {return it.getName().equals("<clinit>");}

            public Boolean doCall() {
                return doCall(null);
            }

        });
        if (!DefaultGroovyMethods.asBoolean(methodNode)) {
            methodNode = new MethodNode("<clinit>", Opcodes.ACC_STATIC, ClassHelper.VOID_TYPE, new Parameter[0], new ClassNode[0], new BlockStatement());
            methodNode.setSynthetic(true);
            classNode.addMethod(methodNode);
        }

        return methodNode;
    }

    /**
     * Sets location(lineNumber, colNumber, lastLineNumber, lastColumnNumber) for node using standard context information.
     * Note: this method is implemented to be closed over ASTNode. It returns same node as it received in arguments.
     *
     * @param astNode Node to be modified.
     * @param ctx     Context from which information is obtained.
     * @return Modified astNode.
     */
    public static <T extends ASTNode> T setupNodeLocation(T astNode, ParserRuleContext ctx) {
        astNode.setLineNumber(ctx.getStart().getLine());
        astNode.setColumnNumber(ctx.getStart().getCharPositionInLine() + 1);
        astNode.setLastLineNumber(ctx.getStop().getLine());
        astNode.setLastColumnNumber(ctx.getStop().getCharPositionInLine() + 1 + ctx.getStop().getText().length());
        return astNode;
    }

    public static <T extends ASTNode> T setupNodeLocation(T astNode, Token token) {
        astNode.setLineNumber(token.getLine());
        astNode.setColumnNumber(token.getCharPositionInLine() + 1);
        astNode.setLastLineNumber(token.getLine());
        astNode.setLastColumnNumber(token.getCharPositionInLine() + 1 + token.getText().length());
        return astNode;
    }

    public int parseClassModifiers(@NotNull List<GroovyParser.ClassModifierContext> ctxs) {
        List<TerminalNode> visibilityModifiers = new ArrayList<TerminalNode>();
        int modifiers = 0;
        for (int i = 0; i < ctxs.size(); i++) {
            for (Object ctx : ctxs.get(i).children) {
                ParseTree child = null;
                if (ctx instanceof List) {
                    List list = (List)ctx;
                    assert list.size() == 1;
                    child = (ParseTree)list.get(0);
                }
                else
                    child = (ParseTree)ctx;

                assert child instanceof TerminalNode;
                switch (((TerminalNode)child).getSymbol().getType()) {
                case GroovyLexer.VISIBILITY_MODIFIER:
                    DefaultGroovyMethods.leftShift(visibilityModifiers, (TerminalNode)child);
                    break;
                case GroovyLexer.KW_STATIC:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STATIC, (TerminalNode)child);
                    break;
                case GroovyLexer.KW_ABSTRACT:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_ABSTRACT, (TerminalNode)child);
                    break;
                case GroovyLexer.KW_FINAL:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_FINAL, (TerminalNode)child);
                    break;
                case GroovyLexer.KW_STRICTFP:
                    modifiers |= checkModifierDuplication(modifiers, Opcodes.ACC_STRICT, (TerminalNode)child);
                    break;
                }
            }
        }

        if (DefaultGroovyMethods.asBoolean(visibilityModifiers))
            modifiers |= parseVisibilityModifiers(visibilityModifiers, 0);
        else modifiers |= Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;
        return modifiers;
    }

    public int checkModifierDuplication(int modifier, int opcode, TerminalNode node) {
        if ((modifier & opcode) == 0) return modifier | opcode;
        else {
            Token symbol = node.getSymbol();

            Integer line = symbol.getLine();
            Integer col = symbol.getCharPositionInLine() + 1;
            sourceUnit.addError(new SyntaxException("Cannot repeat modifier: " + symbol.getText() + " at line: " + String.valueOf(line) + " column: " + String.valueOf(col) + ". File: " + sourceUnit.getName(), line, col));
            return modifier;
        }

    }

    /**
     * Traverse through modifiers, and combine them in one int value. Raise an error if there is multiple occurrences of same modifier.
     *
     * @param ctxList                   modifiers list.
     * @param defaultVisibilityModifier Default visibility modifier. Can be null. Applied if providen, and no visibility modifier exists in the ctxList.
     * @return tuple of int modifier and boolean flag, signalising visibility modifiers presence(true if there is visibility modifier in list, false otherwise).
     * @see #checkModifierDuplication(int, int, org.antlr.v4.runtime.tree.TerminalNode)
     */
    public ArrayList<Object> parseModifiers(List<GroovyParser.MemberModifierContext> ctxList, Integer defaultVisibilityModifier) {
        final int[] modifiers = new int[1]; // need to be final,
        final boolean[] hasVisibilityModifier = { false };
        DefaultGroovyMethods.each(ctxList, new Closure<Void>(this, this) {
            public Void doCall(GroovyParser.MemberModifierContext it) {
                TerminalNode child = (DefaultGroovyMethods.asType(it.getChild(0), TerminalNode.class));
                switch (child.getSymbol().getType()) {
                case GroovyLexer.KW_STATIC:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_STATIC, child);
                    break;
                case GroovyLexer.KW_ABSTRACT:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_ABSTRACT, child);
                    break;
                case GroovyLexer.KW_FINAL:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_FINAL, child);
                    break;
                case GroovyLexer.KW_NATIVE:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_NATIVE, child);
                    break;
                case GroovyLexer.KW_SYNCHRONIZED:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_SYNCHRONIZED, child);
                    break;
                case GroovyLexer.KW_TRANSIENT:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_TRANSIENT, child);
                    break;
                case GroovyLexer.KW_VOLATILE:
                    modifiers[0] |= checkModifierDuplication(modifiers[0], Opcodes.ACC_VOLATILE, child);
                    break;
                case GroovyLexer.VISIBILITY_MODIFIER:
                    modifiers[0] |= parseVisibilityModifiers(child);
                    hasVisibilityModifier[0] = true;
                    break;
                }
                return null;
            }

            public Void doCall() {
                return doCall(null);
            }

        });
        if (!hasVisibilityModifier[0] && defaultVisibilityModifier != null) modifiers[0] |= defaultVisibilityModifier;

        return new ArrayList<Object>(Arrays.asList(modifiers[0], hasVisibilityModifier[0]));
    }

    /**
     * Traverse through modifiers, and combine them in one int value. Raise an error if there is multiple occurrences of same modifier.
     *
     * @param ctxList                   modifiers list.
     * @param defaultVisibilityModifier Default visibility modifier. Can be null. Applied if providen, and no visibility modifier exists in the ctxList.
     * @return tuple of int modifier and boolean flag, signalising visibility modifiers presence(true if there is visibility modifier in list, false otherwise).
     * @see #checkModifierDuplication(int, int, org.antlr.v4.runtime.tree.TerminalNode)
     */
    public ArrayList<Object> parseModifiers(List<GroovyParser.MemberModifierContext> ctxList) {
        return parseModifiers(ctxList, null);
    }

    public void reportError(String text, int line, int col) {
        sourceUnit.addError(new SyntaxException(text, line, col));
    }

    public static int parseVisibilityModifiers(TerminalNode modifier) {
        assert modifier.getSymbol().getType() == GroovyLexer.VISIBILITY_MODIFIER;
        if (DefaultGroovyMethods.isCase("public", modifier.getSymbol().getText()))
            return Opcodes.ACC_PUBLIC;
        else if (DefaultGroovyMethods.isCase("private", modifier.getSymbol().getText()))
            return Opcodes.ACC_PRIVATE;
        else if (DefaultGroovyMethods.isCase("protected", modifier.getSymbol().getText()))
            return Opcodes.ACC_PROTECTED;
        else
            throw new AssertionError(modifier.getSymbol().getText() + " is not a valid visibility modifier!");
    }

    public int parseVisibilityModifiers(List<TerminalNode> modifiers, int defaultValue) {
        if (!DefaultGroovyMethods.asBoolean(modifiers)) return defaultValue;

        if (modifiers.size() > 1) {
            Token modifier = modifiers.get(1).getSymbol();

            Integer line = modifier.getLine();
            Integer col = modifier.getCharPositionInLine() + 1;

            reportError("Cannot specify modifier: " + modifier.getText() + " when access scope has already been defined at line: " + String.valueOf(line) + " column: " + String.valueOf(col) + ". File: " + sourceUnit.getName(), line, col);
        }


        return parseVisibilityModifiers(modifiers.get(0));
    }

    /**
     * Method for construct string from string literal handling empty strings.
     *
     * @param node
     * @return
     */
    public static String parseString(TerminalNode node) {
        String t = node.getText();
        return DefaultGroovyMethods.asBoolean(t) ? DefaultGroovyMethods.getAt(t, new IntRange(true, 1, -2)) : t;
    }

    public static Object initialExpressionForType(ClassNode type) {
        if (ClassHelper.int_TYPE.equals(type))
            return 0;
        else if (ClassHelper.long_TYPE.equals(type))
            return 0L;
        else if (ClassHelper.double_TYPE.equals(type))
            return 0.0;
        else if (ClassHelper.float_TYPE.equals(type))
            return 0f;
        else if (ClassHelper.boolean_TYPE.equals(type))
            return Boolean.FALSE;
        else if (ClassHelper.short_TYPE.equals(type))
            return (short)0;
        else if (ClassHelper.byte_TYPE.equals(type))
            return (byte)0;
        else if (ClassHelper.char_TYPE.equals(type))
            return (char)0;
        else return null;
    }

    public ModuleNode getModuleNode() {
        return moduleNode;
    }

    public void setModuleNode(ModuleNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    private ModuleNode moduleNode;
    private SourceUnit sourceUnit;
    private ClassLoader classLoader;
    private static ASTBuilder instance;
    private Stack<ClassNode> classes = new Stack<ClassNode>();
    private Stack<List<InnerClassNode>> innerClassesDefinedInMethod = new Stack<List<InnerClassNode>>();
    private int anonymousClassesCount = 0;
}
