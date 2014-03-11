package vct.antlr4.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import vct.col.ast.*;
import vct.col.ast.ASTSpecial.Kind;
import vct.col.ast.PrimitiveType.Sort;
import vct.parsers.JavaJMLLexer;
import vct.parsers.JavaLexer;
import vct.parsers.JavaParser.BlockContext;
import vct.parsers.JavaParser.ClassBodyContext;
import vct.parsers.JavaParser.ClassBodyDeclarationContext;
import vct.parsers.JavaParser.LiteralContext;
import vct.util.Syntax;
import static hre.System.*;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Convert the shared parts of JML and Java parse trees to COL.
 *
 * @author <a href="mailto:s.c.c.blom@utwente.nl">Stefan Blom</a>
*/
public class AbstractJavaToCol extends ANTLRtoCOL {

  private final int IntegerLiteral;
  private final int StringLiteral;
  
  public AbstractJavaToCol(Syntax syntax, String filename,
      BufferedTokenStream tokens, Parser parser, int identifier, int comment, Class<?> lexer_class) {
    super(syntax, filename, tokens, parser, identifier, lexer_class);
    IntegerLiteral=getStaticInt(lexer_class,"IntegerLiteral");
    StringLiteral=getStaticInt(lexer_class,"StringLiteral");
  }
  
  public ASTNode getLocalVariableDeclaration(ParserRuleContext ctx) {
    int N=ctx.getChildCount();
    ASTNode list[]=convert_range(ctx,0,N-1);
    return getVariableDeclaration((ParserRuleContext)ctx.getChild(N-1),list);
  }
  
  public ASTNode getQualifiedName(ParserRuleContext ctx) {
    ASTNode n[]=convert_list(ctx,".");
    ASTNode res=n[0];
    for(int i=1;i<n.length;i++){
      if (!(n[i] instanceof NameExpression)) return null;
      String field=((NameExpression)n[i]).getName();
      res=create.dereference(res, field);
    }
    return res;
  }

  public ASTNode getExpression(ParserRuleContext ctx){
    if (match(ctx,null,":",null)){
      String label=getIdentifier(ctx,0);
      ASTNode res=convert(ctx,2);
      res.labeled(label);
      return res;
    }
    if (match(ctx,null,".",null)){
      return create.dereference(convert(ctx,0),getIdentifier(ctx,2));
    }
    if (match(ctx,null,"(",")")||match(ctx,null,"(",null,")")){
      ASTList before=new ASTList();
      scan_comments_before(before,ctx.getChild(1));
      ASTList after=new ASTList();
      scan_comments_after(after,ctx);
      ASTNode om=convert(ctx,0);
      ASTNode args[];
      if (match(ctx,null,"(",")")){
        args=new ASTNode[0];
      } else {
        args=convert_list((ParserRuleContext)ctx.getChild(2),",");
      }
      ASTNode object=null;
      String method;
      if (om instanceof NameExpression){
        object=null;
        method=((NameExpression)om).getName();
      } else if (om instanceof Dereference){
        object=((Dereference)om).object;
        method=((Dereference)om).field;
      } else {
        throw hre.System.Failure("could not convert %s to object/method at %s",om.getClass(),om.getOrigin());
      }
      MethodInvokation res= create.invokation(object, null, method, args);
      for(ASTNode n:before){
        res.get_before().add(n);
      }
      for(ASTNode n:after){
        res.get_after().add(n);
      }
      return res;
    }
    if (match(ctx,"new",null)){
      return convert(ctx,1); 
    }
    if (match(ctx,null,"?",null,":",null)){
      return create.expression(StandardOperator.ITE,convert(ctx,0),convert(ctx,2),convert(ctx,4));
    }
    return null;    
  }
  
  public ASTNode getType(ParserRuleContext ctx) {
    if (match(ctx,null,"[","]")){
      return create.primitive_type(Sort.Array, checkType(convert(ctx,0)));
    }
    if (match(ctx,null,"->",null)){
      Type left=checkType(convert(ctx,0));
      if(left instanceof TupleType){
        return create.arrow_type(((TupleType)left).types,checkType(convert(ctx,2)));
      } else {
        return create.arrow_type(left,checkType(convert(ctx,2)));
      }
    }
    if (match(ctx,null,",",null)){
      ArrayList<Type> types=new ArrayList();
      getTuple(types,ctx);
      return create.tuple_type(types.toArray(new Type[0]));
    }
    return null;
  }
  private void getTuple(ArrayList<Type> types, ParserRuleContext ctx) {
    ParseTree left=ctx.getChild(0);
    if (left instanceof ParserRuleContext && match((ParserRuleContext)left,null,",",null)){
      getTuple(types,(ParserRuleContext)left);
    } else {
      types.add(checkType(convert(ctx,0)));
    }
    types.add(checkType(convert(ctx,2)));
  }
  public ASTNode getCreatedName(ParserRuleContext ctx) {
    if (match(ctx,(String)null)){
      String name=getIdentifier(ctx,0);
      return create.class_type(name);
    } else if (match(ctx,(String)null,"TypeArgumentsOrDiamond")) {
      String name=getIdentifier(ctx,0);
      ASTNode args[]=convert_list((ParserRuleContext)(((ParserRuleContext)ctx.getChild(1)).getChild(0)), "<", ",", ">");
      return create.class_type(name, args);
    } else {
      throw MissingCase(ctx);
    }
  }
  public ASTNode getArrayInitializer(ParserRuleContext ctx) {
    ASTNode n[]=convert_list(ctx,"{",",","}");
    return create.expression(StandardOperator.Build,n);
  }
  public ASTNode getCreator(ParserRuleContext ctx){
    if (match(ctx,null,"ClassCreatorRestContext")){
      ParserRuleContext rest_ctx=(ParserRuleContext)ctx.getChild(1);
      Type type=checkType(convert(ctx,0));
      //String name=getIdentifier(ctx,0);
      if (match(rest_ctx,"ArgumentsContext")){
        ParserRuleContext args_ctx=(ParserRuleContext)rest_ctx.getChild(0);
        ASTNode args[];
        if (args_ctx.getChildCount()>2){
          args=convert_list((ParserRuleContext)args_ctx.getChild(1),",");
        } else {
          args=new ASTNode[0];
        }
        MethodInvokation res=create.new_object(type/*create.class_type(name)*/, args);
        scan_comments_after(res.get_before(),ctx.getChild(0));
        scan_comments_after(res.get_after(),ctx);
        return res;
      }
      Debug("no arguments");
    }
    Debug("no class creator");
    return null;
  }
  public Type checkType(ASTNode n){
    if (n instanceof Type) {
      return (Type)n;
    }
    if (n instanceof NameExpression){
      return create.class_type(n.getOrigin(),((NameExpression)n).getName());
    }
    throw hre.System.Failure("%d node at %s is not a type",n.getClass(),n.getOrigin());
  }
  protected DeclarationStatement[] getFormalParameters(ParseTree tree){
    DeclarationStatement args[];
    ParserRuleContext arg_ctx=(ParserRuleContext)tree;
    if (match(arg_ctx,"(",")")) {
      args=new DeclarationStatement[0];
    } else {
      ASTNode tmp[]=convert_list((ParserRuleContext)arg_ctx.getChild(1),",");
      args=new DeclarationStatement[tmp.length];
      for(int i=0;i<tmp.length;i++){
        args[i]=(DeclarationStatement)tmp[i];
      }
    }
    return args;
  }
  public ClassType forceClassType(ASTNode convert) {
    if (convert instanceof ClassType) return (ClassType)convert;
    if (convert instanceof NameExpression) return create.class_type(convert.toString());
    throw hre.System.Failure("cannot convert %s to ClassType",convert.getClass());
  }
  
  public ASTNode getImportDeclaration(ParserRuleContext ctx){
    if (match(ctx,"import",null,";")){
      return create.special(Kind.Import,convert(ctx,1));
    }
    return null;
  }
  public Method getConstructorDeclaration(ParserRuleContext ctx) {
    String name=getIdentifier(ctx,0);
    DeclarationStatement args[]=getFormalParameters(ctx.getChild(1));
    Type returns=create.primitive_type(Sort.Void);
    if (ctx.getChildCount()==3){
      ASTNode body=convert(ctx,2);
      return create.method_kind(Method.Kind.Constructor, returns, null, name, args, body);
    } else {
      return null;
    }
  }
  public Method getMethodDeclaration(ParserRuleContext ctx) {
    int N=ctx.getChildCount();
    Type t;
    if (match(0,true,ctx,"void")){
      t=create.primitive_type(Sort.Void);
    } else {
      t=checkType(convert(ctx,0));
    }
    String name=getIdentifier(ctx,1);
    DeclarationStatement args[]=getFormalParameters(ctx.getChild(2));
    ASTNode body;
    if (match(N-1,true,ctx,";")){
      body=null;
    } else {
      body=convert(ctx,N-1);
    }
    Method res=create.method_decl(t,null, name, args, body);
    res.setStatic(false);
    return res;
  }
  public BlockStatement getBlock(ParserRuleContext ctx) {
    BlockStatement res=create.block();
    scan_body(res,ctx);
    return res;
  }

  public void scan_body(ASTSequence<?> cl, ParserRuleContext ctx) {
    int N=ctx.getChildCount()-1;
    for(int i=1;i<N;i++){
      if (match(i,true,ctx,";")) {
        scan_comments_before(cl,ctx.getChild(i));
        continue;
      }
      ASTNode tmp=convert(ctx,i);
      scan_comments_before(cl,ctx.getChild(i));
      cl.add(tmp);
    }
    scan_comments_before(cl,ctx.getChild(N));
  }

  protected ASTNode convert_annotated(ParserRuleContext ctx) {
    ASTList list=new ASTList();
    int N=ctx.children.size()-1;
    for(int i=0;i<N;i++){
      list.add(convert(ctx,i));
      scan_comments_after(list, ctx.getChild(i));
    }
    ASTNode res=convert(ctx,N);
    res.attach();
    for (ASTNode n:list){
      Debug("adding %s annotation",n.getClass());
      res.attach(n);
    }
    return res;
  }

  public ASTNode getClassBodyDeclaration(ParserRuleContext ctx) {
    if (match(ctx,"static","BlockContext")){
      ASTNode res=convert(ctx,1);
      res.setStatic(true);
      return res;
    }
    if (match(ctx,"BlockContext")){
      return convert(ctx,0);
    }
    return convert_annotated(ctx);
  }

  public ASTNode getStatement(ParserRuleContext ctx) {
    if (match(ctx,"if",null,null)){
      return create.ifthenelse(convert(ctx,1),convert(ctx,2));
    }
    if (match(ctx,"if",null,null,"else",null)){
      return create.ifthenelse(convert(ctx,1),convert(ctx,2),convert(ctx,4));
    }
    if (match(ctx,"return",";")){
      return create.return_statement();
    }
    if (match(ctx,"return",null,";")){
      ReturnStatement res=create.return_statement(convert(ctx,1));
      scan_comments_after(res.get_after(),ctx.getChild(1));
      return res;
    }
    if (match(ctx,"Expression",";")){
      return create.special(ASTSpecial.Kind.Expression,convert(ctx,0)); 
    }
    if (match(ctx,"while",null,null)){
      LoopStatement res=create.while_loop(convert(ctx,1),convert(ctx,2));
      scan_comments_after(res.get_after(), ctx.getChild(1));
      return res;
    }
    if (match(ctx,"for","(",null,")",null)){
      //ASTNode init=convert(ctx,2);
      //ASTNode test=convert(ctx,4);
      //ASTNode update=convert(ctx,6);
      ASTNode body=convert(ctx,4);
      //LoopStatement res=create.for_loop(init, test, update, body);
      LoopStatement res=create.while_loop(create.constant(true),body);
      scan_comments_after(res.get_after(), ctx.getChild(3));
      return res;
    }
    if (match(ctx,"assert",null,";")){
      return create.special(ASTSpecial.Kind.Assert,convert(ctx,1));
    }
    if (match(ctx,"throw",null,";")){
      return create.special(ASTSpecial.Kind.Throw,convert(ctx,1));
    }
    if (match(0,true,ctx,"try",null)){
      Warning("ignoring catch and/or finally");
      return convert(ctx,1);
    }
    if (match(ctx,null,":",null)){
      ASTNode res=convert(ctx,2);
      String label=getIdentifier(ctx,0);
      res.labeled(label);
      return res;
    }
    return null;
  }

  public ASTNode getVariableDeclaration(ParserRuleContext var_ctx,ASTNode ... list){
    int N=list.length-1;
    ASTNode vars[]=convert_list(var_ctx,",");
    VariableDeclaration decl=create.variable_decl(checkType(list[N]));
    for(int i=0;i<vars.length;i++){
      DeclarationStatement tmp;
      if (vars[i] instanceof NameExpression){
        tmp=create.field_decl(((NameExpression)vars[i]).getName(),checkType(list[N]));
      } else if (vars[i] instanceof DeclarationStatement) {
        DeclarationStatement d=(DeclarationStatement)vars[i];
        tmp=create.field_decl(d.getName(),checkType(list[N]),d.getInit());
      } else {
        throw hre.System.Failure("unexpected %s in variable list at %s",vars[i].getClass(),create.getOrigin());
      }
      decl.add(tmp);
    }
    for(int i=0;i<N;i++){
      decl.attach(list[i]);
    }
    return decl;
  }
  
  public DeclarationStatement getVariableDeclaratorId(ParserRuleContext ctx){
    String name=getIdentifier(ctx,0);
    Type t=create.class_type("basetype");
    if (match(ctx,null,"[","]")){
      t=create.primitive_type(PrimitiveType.Sort.Array,t);
    }
    return create.field_decl(name, t);
  }
  
  public DeclarationStatement getVariableDeclarator(ParserRuleContext ctx) {
    if (match(ctx,null,"=",null)){
      DeclarationStatement decl=(DeclarationStatement)convert(ctx,0);
      ASTNode expr=convert(ctx,2);
      return create.field_decl(decl.name,decl.getType(),expr);
    }
    return null;
  }
  public ASTNode getLiteral(ParserRuleContext ctx) {
    Token tok=((TerminalNode)ctx.getChild(0)).getSymbol();
    int t=tok.getType();
    if (t==IntegerLiteral){
      return create.constant(Integer.parseInt(tok.getText()));
    }
    if (t==StringLiteral){
      String text=tok.getText();
      return create.constant(StringEscapeUtils.unescapeJava(text.substring(1,text.length()-1)));
    }
    if (match(ctx,"true")) return create.constant(true);
    if (match(ctx,"false")) return create.constant(false);
    return null;
  }
  public ASTNode getClassOrInterfaceType(ParserRuleContext ctx) {
    if (match(ctx,null,".",null)){
      String name[]=new String[2];
      name[0]=getIdentifier(ctx,0);
      name[1]=getIdentifier(ctx,2);
      return create.class_type(name);
    }
    if (match(ctx,null,"TypeArgumentsContext")){
      String name=getIdentifier(ctx,0);
      ParserRuleContext arg_ctx=(ParserRuleContext)ctx.getChild(1);
      ASTNode args[]=convert_list(arg_ctx,1,arg_ctx.getChildCount()-1,",");
      return create.class_type(name, args);
    }
    ASTNode names[]=convert_list(ctx, ".");
    if (names!=null){
      String name[]=new String[names.length];
      for(int i=0;i<name.length;i++){
        name[i]=names[i].toString();
      }
      return create.class_type(name);
    }
    return null;
  }
  public DeclarationStatement getFormalParameter(ParserRuleContext ctx) {
    if (match(ctx,null,null)){
      VariableDeclaration decl=create.variable_decl(checkType(convert(ctx,0)));
      DeclarationStatement var=getVariableDeclaratorId((ParserRuleContext)ctx.getChild(1));
      decl.add(var);
      DeclarationStatement vars[]=decl.flatten();
      if (vars.length==1) return vars[0];
    }
    return null;
  }
  
  public ASTClass getClassDeclaration(ParserRuleContext ctx){
    int N=ctx.getChildCount()-1;
    ClassType[]bases=null;
    ClassType[]supports=null;
    ContractBuilder cb=new ContractBuilder();
    //Warning("class decl %s",ctx.toStringTree(parser));
    for(int i=2;i<N;i++){
      //Warning("i==%d",i);
      if (match(i,true,ctx,"extends",null)){
        bases=new ClassType[]{forceClassType(convert(ctx,i+1))};
        i+=1;
      } else if (match(i,true,ctx,"TypeParametersContext")){
        ParserRuleContext pars=(ParserRuleContext)ctx.getChild(i);
        int K=pars.getChildCount();
        for(int k=1;k<K;k+=2){
          doParameter(cb,pars.getChild(k));
        }
      } else {
        return null;
      }
    }
    ASTClass cl=create.ast_class(getIdentifier(ctx,1), ASTClass.ClassKind.Plain, bases , supports );
    scan_body(cl,(ClassBodyContext)ctx.getChild(N));
    cl.setContract(cb.getContract());
    return cl;
  }

  private void doParameter(ContractBuilder cb, ParseTree tree) {
    enter(tree);
    Debug("converting type parameter %s",tree.toStringTree(parser));
    if (tree instanceof ParserRuleContext) {
      ParserRuleContext ctx=(ParserRuleContext)tree;
      if (instance(ctx,"TypeParameter")){
        cb.given(create.field_decl(getIdentifier(ctx,0),create.primitive_type(Sort.Class)));
      //} else if (match(ctx,"")){
        
      } else {
        Abort("missing case %s",ctx.toStringTree(parser));
      }
    } else {
      Abort("missing case");
    }
    leave(tree,null);
  }
}
