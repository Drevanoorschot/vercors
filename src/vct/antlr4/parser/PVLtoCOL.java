package vct.antlr4.parser;

import static hre.System.Debug;
import static hre.System.Fail;
import static hre.System.Warning;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import pv.parser.PVFullLexer;
import pv.parser.PVFullParser;
import pv.parser.PVFullParser.Abs_declContext;
import pv.parser.PVFullParser.ArgsContext;
import pv.parser.PVFullParser.BlockContext;
import pv.parser.PVFullParser.ClassTypeContext;
import pv.parser.PVFullParser.ClazContext;
import pv.parser.PVFullParser.ConstructorContext;
import pv.parser.PVFullParser.ContractContext;
import pv.parser.PVFullParser.ExprContext;
import pv.parser.PVFullParser.Fence_listContext;
import pv.parser.PVFullParser.FieldContext;
import pv.parser.PVFullParser.FunctionContext;
import pv.parser.PVFullParser.InvariantContext;
import pv.parser.PVFullParser.KernelContext;
import pv.parser.PVFullParser.Kernel_fieldContext;
import pv.parser.PVFullParser.LexprContext;
import pv.parser.PVFullParser.MethodContext;
import pv.parser.PVFullParser.ProgramContext;
import pv.parser.PVFullParser.StatementContext;
import pv.parser.PVFullParser.TupleContext;
import pv.parser.PVFullParser.TypeArgsContext;
import pv.parser.PVFullParser.TypeContext;
import pv.parser.PVFullParser.ValuesContext;
import pv.parser.PVFullVisitor;
import vct.col.ast.ASTClass;
import vct.col.ast.ASTNode;
import vct.col.ast.ASTReserved;
import vct.col.ast.ASTSpecial;
import vct.col.ast.BeforeAfterAnnotations;
import vct.col.ast.BlockStatement;
import vct.col.ast.CompilationUnit;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.Dereference;
import vct.col.ast.Method;
import vct.col.ast.NameExpression;
import vct.col.ast.ParallelBarrier;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;
import vct.col.ast.Type;
import vct.col.ast.ASTClass.ClassKind;
import vct.col.ast.Method.Kind;
import vct.col.ast.PrimitiveType.Sort;
import vct.col.ast.VariableDeclaration;
import vct.util.Syntax;
import static vct.col.ast.ASTReserved.*;

/**
 * Convert ANTLR parse trees for PVL to COL.
 * 
 * @author <a href="mailto:s.c.c.blom@utwente.nl">Stefan Blom</a>
*/
public class PVLtoCOL extends ANTLRtoCOL implements PVFullVisitor<ASTNode> {

  public static CompilationUnit convert(ParseTree tree, String file_name,BufferedTokenStream tokens,org.antlr.v4.runtime.Parser parser) {
    CompilationUnit unit=new CompilationUnit(file_name);
    PVLtoCOL visitor=new PVLtoCOL(PVLSyntax.get(),file_name,tokens,parser);
    visitor.scan_to(unit,tree);
    return unit;
  }
  public PVLtoCOL(Syntax syntax, String filename, BufferedTokenStream tokens,org.antlr.v4.runtime.Parser parser) {
    super(syntax, filename, tokens,parser,PVFullLexer.ID,PVFullLexer.class);
  }

  @Override
  public ASTNode visitClaz(ClazContext ctx) {
    Contract c;
    if (((ParserRuleContext)ctx.getChild(0)).children==null){
      c=null;
    } else {
      c=(Contract)convert(ctx,0);
    }
    String name=getIdentifier(ctx,2);
    ASTClass cl=create.ast_class(name,ClassKind.Plain,null,null);
    for(int i=4;i<ctx.children.size()-1;i++){
      ASTNode node=convert(ctx.children.get(i));
      if (node.isValidFlag(ASTNode.STATIC) && node.isStatic()){
        cl.add_static(node);
      } else {
        cl.add_dynamic(node);
      } 
    }
    cl.setContract(c);
    return cl;
  }

  @Override
  public ASTNode visitContract(ContractContext ctx) {
    ContractBuilder cb=new ContractBuilder();
    Debug("contract %s",ctx.toStringTree());
    if (ctx.children!=null){
      int N=ctx.children.size();
      for(int i=0;i<N;){
        switch(ctx.children.get(i).toString()){
        case "modifies":
          cb.modifies(convert(ctx.children.get(i+1)));
          i+=3;
          break;
        case "requires":
          cb.requires(convert(ctx.children.get(i+1)));
          i+=3;
          break;
        case "ensures":
          cb.ensures(convert(ctx.children.get(i+1)));
          i+=3;
          break;
        case "given":{
          Type type=(Type)convert(ctx.children.get(i+1));
          String name=getIdentifier(ctx, i+2);
          if (ctx.children.get(i+3).toString().equals(";")){
            i+=4;
            cb.given(create.field_decl(name, type));
            break;
          }
          Fail("missing case in %s",ctx.children.get(i));
        }
        case "yields":{
          Type type=(Type)convert(ctx.children.get(i+1));
          String name=getIdentifier(ctx, i+2);
          if (ctx.children.get(i+3).toString().equals(";")){
            i+=4;
            cb.yields(create.field_decl(name, type));
            break;
          }
          Fail("missing case in %s",ctx.children.get(i));
        }
        default:
          Fail("missing case: %s",ctx.children.get(i));
        }
      }
    }
    return cb.getContract(false);
  }

  @Override
  public ASTNode visitArgs(ArgsContext ctx) {
    throw hre.System.Failure("illegal call to visitArgs");
  }
  
  private DeclarationStatement[] convertArgs(ArgsContext ctx){
    ArrayList<DeclarationStatement> args=new ArrayList<DeclarationStatement>();
    DeclarationStatement empty[]=new DeclarationStatement[0];
    if (ctx.children==null) return empty;
    int N=ctx.children.size();
    for(int i=0;i<N;i+=3){
      Type type=(Type)convert(ctx.children.get(i));
      String name=getIdentifier(ctx, i+1);
      args.add(create.field_decl(name, type));
    }
    return args.toArray(empty);
  }

  @Override
  public ASTNode visitTuple(TupleContext ctx) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ASTNode visitFence_list(Fence_listContext ctx) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ASTNode visitBlock(BlockContext ctx) {
    BlockStatement block=create.block();
    int N=ctx.children.size()-1;
    for(int i=1;i<N;i++){
      block.add_statement(convert(ctx.children.get(i)));
      ParseTree tmp=ctx.children.get(i);
    }
    return block;
  }

  @Override
  public ASTNode visitExpr(ExprContext ctx) {
    if (ctx.children.size()==1){
      ASTNode res=try_specials(ctx.children.get(0).toString());
      if (res!=null) return res;
      return convert(ctx.children.get(0));
    }
    if (ctx.children.get(0) instanceof TerminalNode){
      switch(ctx.children.get(0).toString()){
      case "\\old":
        return create.expression(StandardOperator.Old,getTuple((ParserRuleContext)ctx.children.get(1)));
      case "Hist":
        return create.expression(StandardOperator.History,getTuple((ParserRuleContext)ctx.children.get(1)));
      case "Perm":
        return create.expression(StandardOperator.Perm,getTuple((ParserRuleContext)ctx.children.get(1)));
      case "Value":
        return create.expression(StandardOperator.Value,getTuple((ParserRuleContext)ctx.children.get(1)));
      case "PointsTo":
        return create.expression(StandardOperator.PointsTo,getTuple((ParserRuleContext)ctx.children.get(1)));
      }
    }
    if (match(ctx,"head",tuple)){
      ASTNode args[]=getTuple((ParserRuleContext)ctx.getChild(1));
      return create.expression(StandardOperator.Head,args);
    }
    if (match(ctx,"tail",tuple)){
      ASTNode args[]=getTuple((ParserRuleContext)ctx.getChild(1));
      return create.expression(StandardOperator.Tail,args);
    }
    if (match(0,true,ctx,"[",null)){
      ASTNode args[]=convert_list(ctx,"[",",","]");
      args[0]=create.primitive_type(Sort.Sequence, args[0]);
      return create.expression(StandardOperator.Build,args); 
    }
    if (match(ctx,"seq","<",null,">",null)){
      Type t=checkType(convert(ctx,2));
      ASTNode args[]=convert_list((ParserRuleContext)ctx.getChild(4),"{",",","}");
      return create.expression(StandardOperator.Build,create.primitive_type(Sort.Sequence,t),args);
    }
    if (match(ctx,"set","<",null,">",null)){
      Type t=checkType(convert(ctx,2));
      ASTNode args[]=convert_list((ParserRuleContext)ctx.getChild(4),"{",",","}");
      return create.expression(StandardOperator.Build,create.primitive_type(Sort.Set,t),args);
    }
    if (match(ctx,"bag","<",null,">",null)){
      Type t=checkType(convert(ctx,2));
      ASTNode args[]=convert_list((ParserRuleContext)ctx.getChild(4),"{",",","}");
      return create.expression(StandardOperator.Build,create.primitive_type(Sort.Bag,t),args);
    }
    if (match(ctx,"ExprContext",".","ExprContext")){
      ASTNode e1=convert(ctx.children.get(0));
      ASTNode e2=convert(ctx.children.get(2));
      return create.dereference(e1,e2.toString());
    }
    if (match(ctx,"!",null)){
      return create.expression(StandardOperator.Not,convert(ctx,1));
    }
    if (match(ctx,null,"?",null,":",null)){
      return create.expression(StandardOperator.ITE,convert(ctx,0),convert(ctx,2),convert(ctx,4));
    }
    if (match(ctx,"(",null,")")){
      return convert(ctx,1);
    }
    if (match(ctx,null,tuple)){
      return get_invokation(ctx,0);
    }
    if (match(ctx,"new",null,tuple)){
      ASTNode args[]=getTuple((ParserRuleContext)ctx.getChild(2));
      String name=getIdentifier(ctx,1);
      return create.invokation(create.class_type(name), null,  name, args);
    }
    if (match(ctx,"new",null,"[",null,"]")){
      Type t=checkType(convert(ctx,1));
      ASTNode size=convert(ctx,3);
      return create.new_array(t,size);
    }
    if (match(ctx,"(","\\forall*",null,null,";",null,";",null,")")){
      return create.starall(convert(ctx,5),convert(ctx,7),create.field_decl(getIdentifier(ctx,3),(Type)convert(ctx,2)));
    }
    if (match(ctx,"(","\\forall",null,null,";",null,";",null,")")){
      return create.forall(convert(ctx,5),convert(ctx,7),create.field_decl(getIdentifier(ctx,3),(Type)convert(ctx,2)));
    }
    if (match(ctx,"(","\\exists",null,null,";",null,";",null,")")){
      return create.exists(convert(ctx,5),convert(ctx,7),create.field_decl(getIdentifier(ctx,3),(Type)convert(ctx,2)));
    }
    if (match(ctx,null,":",null)){
      ASTNode res=convert(ctx,2);
      String name=getIdentifier(ctx,0);
      res.addLabel(create.label(name));
      return res;
    }
    if (match(ctx,null,"with",null)){
      ASTNode tmp=convert(ctx,0);
      if (tmp instanceof BeforeAfterAnnotations){
        BeforeAfterAnnotations res=(BeforeAfterAnnotations)tmp;
        BlockStatement block=(BlockStatement)convert(ctx,2);
        res.set_before(block);
        return tmp;
      } else {
        Fail("%s: with block not allowed here",create.getOrigin());
      }
    }
    if (match(ctx,null,"then",null)){
      ASTNode tmp=convert(ctx,0);
      if (tmp instanceof BeforeAfterAnnotations){
        BeforeAfterAnnotations res=(BeforeAfterAnnotations)tmp;
        BlockStatement block=(BlockStatement)convert(ctx,2);
        res.set_after(block);
        return tmp;
      } else {
        Fail("%s: then block not allowed here",create.getOrigin());
      }
    }
    return visit(ctx);
  }

  @Override
  public ASTNode visitType(TypeContext ctx) {
    ASTNode res=null;
    if (match(ctx,"seq","<",null,">")){
      Type t=checkType(convert(ctx,2));
      return create.primitive_type(Sort.Sequence,t);
    }
    if (match(ctx,"set","<",null,">")){
      Type t=checkType(convert(ctx,2));
      return create.primitive_type(Sort.Set,t);
    }
    if (match(ctx,"bag","<",null,">")){
      Type t=checkType(convert(ctx,2));
      return create.primitive_type(Sort.Bag,t);
    }
    if (match(ctx,null,"<",null,">")) {
      String name=getIdentifier(ctx,0);
      ASTNode arg=convert(ctx,2);
      return create.class_type(name,arg);
    }
    if (match(0,true,ctx,"TerminalNode")){
      switch(ctx.children.get(0).toString()){
        case "boolean": res=create.primitive_type(Sort.Boolean); break;
        case "frac": res=create.primitive_type(Sort.Fraction); break;
        case "zfrac": res=create.primitive_type(Sort.ZFraction); break;
        case "int": res=create.primitive_type(Sort.Integer); break;
        case "resource": res=create.primitive_type(Sort.Resource); break;
        case "void": res=create.primitive_type(Sort.Void); break;
        case "process": res=create.primitive_type(Sort.Process); break;
        default: res=create.class_type(ctx.children.get(0).toString());
      }
    } else if (match(0,true,ctx,"ClassType")) {
      res=checkType(convert(ctx,0));
    } else {
      Fail("unknown type %s",ctx.toStringTree());
    }
    int N=ctx.children.size();
    int i=1;
    while(i<N){
      if (match(i,true,ctx,"[","ExprContext","]")){
        res=create.primitive_type(Sort.Array,res,convert(ctx.children.get(i+1)));
        i+=3;
      } else if (match(i,true,ctx,"[","]")) {
        res=create.primitive_type(Sort.Array,res);
        i+=2;
      } else {
        Fail("unknown type %s",ctx.toStringTree());
      }
    }
    return res;
  }

  @Override
  public ASTNode visitKernel(KernelContext ctx) {
    String name=getIdentifier(ctx,1);
    ASTClass cl=create.ast_class(name,ClassKind.Kernel,null,null);
    for(int i=3;i<ctx.children.size()-1;i++){
      ASTNode tmp=convert(ctx.children.get(i));
      if (tmp.isStatic()){
        cl.add_static(tmp);
      } else {
        cl.add_dynamic(tmp);
      }
    }
    return cl;
  }

  @Override
  public ASTNode visitFunction(FunctionContext ctx) {
    Contract c=(Contract) convert(ctx.children.get(0));
    int offset;
    if (match(1,true,ctx,"static")){
      offset=1;
    } else {
      offset=0;
    }
    Type returns=(Type)convert(ctx.children.get(offset+1));
    String name=getIdentifier(ctx,offset+2);
    DeclarationStatement args[]=convertArgs((ArgsContext) ctx.children.get(offset+4));
    ASTNode body=convert(ctx.children.get(offset+7));
    Kind kind;
    if (returns.isPrimitive(Sort.Resource)) {
      kind=Kind.Predicate;
    } else {
      kind=Kind.Pure;
    }
    ASTNode res=create.method_kind(kind,returns,c, name, args ,body);
    res.setStatic(offset==1);
    return res;
  }
  
  private String type_expr="TypeContext";
  private String tuple="TupleContext";

  @Override
  public ASTNode visitStatement(StatementContext ctx) {
    if (match(ctx,null,"=",null,";")){
      return create.assignment(convert(ctx,0),convert(ctx,2));
    }
    if (match(ctx,"return",null,";")){
      return create.return_statement(convert(ctx,1));
    }
    if (match(ctx,type_expr,null,";")){
      return create.field_decl(getIdentifier(ctx,1),(Type)convert(ctx,0));
    }
    if (match(ctx,type_expr,null,"=",null,";")){
      return create.field_decl(getIdentifier(ctx,1),(Type)convert(ctx,0),convert(ctx,3));
    }
    if (match(ctx,"if","(",null,")",null)){
      return create.ifthenelse(convert(ctx,2),convert(ctx,4));
    }
    if (match(ctx,"if","(",null,")",null,"else",null)){
      return create.ifthenelse(convert(ctx,2),convert(ctx,4),convert(ctx,6));
    }
    if (match(ctx,"action",null,",",null,null)){
      ASTNode process=convert(ctx,1);
      ASTNode action=convert(ctx,3);
      ASTNode block=convert(ctx,4);
      return create.action_block(process,action,block);
    }
    if (match(ctx,"create",null,",",null,";")){
      return create.special(ASTSpecial.Kind.CreateHistory,convert(ctx,1),convert(ctx,3));
    }
    if (match(ctx,"destroy",null,",",null,",",null,";")){
      return create.special(ASTSpecial.Kind.DestroyHistory,convert(ctx,1),convert(ctx,3),convert(ctx,5));
    }
    if (match(ctx,null,"while","(",null,")",null)){
      PVFullParser.InvariantContext inv_ctx=(PVFullParser.InvariantContext)ctx.children.get(0);
      int N = (inv_ctx.children==null) ? 0 : inv_ctx.children.size()/3;
      ASTNode invs[]=new ASTNode[N];
      for(int i=0;i<N;i++){
        invs[i]=convert(inv_ctx,3*i+1);
      }
      return create.while_loop(convert(ctx,3),convert(ctx,5),invs);
    }
    if (match(ctx,"assert",null,";")){
      return create.expression(StandardOperator.Assert,convert(ctx,1));
    }
    if (match(ctx,"assume",null,";")){
      return create.expression(StandardOperator.Assume,convert(ctx,1));
    }
    if (match(ctx,"fork",null,";")){
      return create.expression(StandardOperator.Fork,convert(ctx,1));
    }
    if (match(ctx,"join",null,";")){
      return create.expression(StandardOperator.Join,convert(ctx,1));
    }
    if (match(ctx,"fold",null,";")){
      return create.expression(StandardOperator.Fold,convert(ctx,1));
    }
    if (match(ctx,"unfold",null,";")){
      return create.expression(StandardOperator.Unfold,convert(ctx,1));
    }
    if (match(ctx,"witness",null,";")){
      return create.expression(StandardOperator.Witness,convert(ctx,1));
    }
    //if (match(ctx,null,tuple,";")){
    //  return get_invokation(ctx,0);
    //}
    if (match(ctx,"ExprContext",";")){
      return convert(ctx,0);
    }
    if (match(ctx,"barrier","(",null,")","{",null,"}")){
      ContractBuilder cb=new ContractBuilder();
      EnumSet<ParallelBarrier.Fence> fences=EnumSet.noneOf(ParallelBarrier.Fence.class);
      if (((ParserRuleContext)ctx.children.get(2)).children!=null){
        for (ParseTree item:((ParserRuleContext)ctx.children.get(2)).children){
          String tag=item.toString();
          switch(tag){
          case "local":
            fences.add(ParallelBarrier.Fence.Local);
            continue;
          case "global":
            fences.add(ParallelBarrier.Fence.Global);
            continue;
          default:
            Fail("unknown fence %s",tag);
          }
        }
      }
      Contract c=(Contract)convert(ctx,5);
      return create.barrier(c,fences);
    }
    return visit(ctx);
  }

  @Override
  public ASTNode visitKernel_field(Kernel_fieldContext ctx) {
    ASTNode res;
    if (ctx.children.size()==4){
      res=create.field_decl(getIdentifier(ctx,2),(Type)convert(ctx.children.get(1)));
    } else {
      VariableDeclaration decl=new VariableDeclaration((Type)convert(ctx.children.get(1)));
      int N=ctx.children.size();
      for(int i=2;i<N;i+=2){
        String name=getIdentifier(ctx,i);
        Type t=create.class_type(name);
        decl.add(create.field_decl(name,t));
      }
      decl.setOrigin(create.getOrigin());
      res=decl;
    }
    String keyword=ctx.children.get(0).toString();
    switch(keyword){
    case "global":
      res.setStatic(true);
      break;
    case "local":
      res.setStatic(false);
      break;
    default:
      Fail("bad variable class %s",keyword);
    }
    return res;
  }

  @Override
  public ASTNode visitField(FieldContext ctx) {
    if (ctx.children.size()==3){
      return create.field_decl(getIdentifier(ctx,1),(Type)convert(ctx.children.get(0)));
    } else {
      VariableDeclaration decl=new VariableDeclaration((Type)convert(ctx.children.get(0)));
      int N=ctx.children.size();
      for(int i=1;i<N;i+=2){
        String name=getIdentifier(ctx,i);
        Type t=create.class_type(name);
        decl.add(create.field_decl(name,t));
      }
      decl.setOrigin(create.getOrigin());
      return decl;
    }
  }

  @Override
  public ASTNode visitInvariant(InvariantContext ctx) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ASTNode visitLexpr(LexprContext ctx) {
    ASTNode res=convert(ctx,0);
    int N=ctx.children.size();
    for(int i=1;i<N;){
      if (match(i,true,ctx,".",null)){
        res=create.dereference(res,getIdentifier(ctx,i+1));
        i+=2;
      } else if(match(i,true,ctx,"[",null,"]")){
        res=create.expression(StandardOperator.Subscript,res,convert(ctx,i+1));
        i+=3;
      } else {
        Fail("unknown lexpr");
      }
    }
    return res;
  }

  @Override
  public ASTNode visitProgram(ProgramContext ctx) {
    /*
    for(ParseTree item:ctx.children){
      if (item instanceof ClazContext || item instanceof KernelContext){
        ASTClass cl=(ASTClass)convert(item);
        unit.add(cl);
      } else {
        Fail("cannot handle %s at top level",item.getClass());
      }
    }
*/
    return null;
  }

  @Override
  public ASTNode visitMethod(MethodContext ctx) {
    Contract c=(Contract) convert(ctx.children.get(0));
    Type returns=(Type)convert(ctx.children.get(1));
    String name=getIdentifier(ctx,2);
    DeclarationStatement args[]=convertArgs((ArgsContext) ctx.children.get(4));
    ASTNode body=convert(ctx.children.get(6));
    ASTNode res=create.method_decl(returns,c, name, args ,body);
    res.setStatic(false);
    return res;
  }

  private ASTNode get_invokation(ParserRuleContext ctx,int ofs) {
    ASTNode f=convert(ctx.children.get(ofs));
    ASTNode args[]=getTuple((ParserRuleContext)ctx.children.get(ofs+1));
    if (f instanceof Dereference){
      Dereference fd=(Dereference) f;
      return create.invokation(fd.object,null,fd.field,args);
    } else if (f instanceof NameExpression){
      return create.invokation(null,null,((NameExpression)f).getName(),args);
    } else {
      throw hre.System.Failure("unimplemented invokation");
    }
  }
  
  private ASTNode[] getTuple(ParserRuleContext ctx){
    int N=(ctx.children.size()-1)/2;
    ASTNode res[]=new ASTNode[N];
    for(int i=0;i<N;i++){
      res[i]=convert(ctx,2*i+1);
    }
    return res;
  }

  private ASTNode try_specials(String text){
    ASTReserved res=syntax.reserved(text);
    if (res!=null){
      return create.reserved_name(res);
    }
    switch(text){
    case "tcount":
    case "gsize":
    case "tid":
    case "gid":
    case "lid":
      return create.unresolved_name(text);
    case "this":
      return create.reserved_name(This);
    case "\\result": return create.reserved_name(Result);
    case "null": return create.reserved_name(Null);
    case "true": return create.constant(true);
    case "false": return create.constant(false);
    }
    return null;
  }
  
  @Override
  public ASTNode visitTerminal(TerminalNode node){
    Token tok=node.getSymbol();
    ASTNode res=try_specials(tok.getText());
    if (res!=null) return res;
    switch(tok.getType()){
    case PVFullLexer.ID:
      return create.unresolved_name(tok.getText());
    case PVFullLexer.NUMBER:
      return create.constant(Integer.parseInt(tok.getText()));
    }
    Fail("At %s: unimplemented terminal node",create.getOrigin());
    return visit(node);
  }
  @Override
  public ASTNode visitConstructor(ConstructorContext ctx) {
    Contract c=(Contract) convert(ctx.children.get(0));
    String name=getIdentifier(ctx,1);
    DeclarationStatement args[]=convertArgs((ArgsContext) ctx.children.get(3));
    ASTNode body=convert(ctx.children.get(5));
    Type returns=create.primitive_type(Sort.Void);
    ASTNode res=create.method_kind(Kind.Constructor,returns,c, name, args ,body);
    res.setStatic(false);
    return res;
  }
  
  @Override
  public ASTNode visitTypeArgs(TypeArgsContext ctx) {
    // TODO Auto-generated method stub
    return null;
  }
  @Override
  public ASTNode visitClassType(ClassTypeContext ctx) {
    String name=getIdentifier(ctx,0);
    ASTNode args[];
    if (ctx.children.size() >1){
      args=convert_list((ParserRuleContext)ctx.getChild(1),"<",",",">");
    } else {
      args=new ASTNode[0];
    }
    return create.class_type(name,args);
  }
  @Override
  public ASTNode visitValues(ValuesContext ctx) {
    if (match(0,true,ctx,"{",null)){
      ASTNode args[]=convert_list(ctx,"{",",","}");
      Type t=create.primitive_type(Sort.Set,create.primitive_type(Sort.Location));
      return create.expression(StandardOperator.Build,t,args); 
    }
    return null;
  }
  @Override
  public ASTNode visitAbs_decl(Abs_declContext ctx) {
    Contract c=(Contract) convert(ctx.children.get(0));
    Type returns=(Type)convert(ctx.children.get(1));
    String name=getIdentifier(ctx,2);
    DeclarationStatement args[]=convertArgs((ArgsContext) ctx.children.get(4));
    ASTNode res=create.method_decl(returns,c, name, args , null);
    res.setStatic(false);
    return res;
  }

}
