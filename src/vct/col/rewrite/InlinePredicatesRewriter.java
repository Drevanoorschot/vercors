package vct.col.rewrite;

import java.util.HashMap;

import vct.col.ast.ASTFlags;
import vct.col.ast.ASTNode;
import vct.col.ast.ASTReserved;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.NameExpression;
import vct.col.ast.OperatorExpression;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;

public class InlinePredicatesRewriter extends AbstractRewriter {

  public final boolean auto;
  
  public InlinePredicatesRewriter(ProgramUnit source,boolean auto) {
    super(source);
    this.auto=auto;
  }

  
  @Override
  public void visit(MethodInvokation e){
    Method def=e.getDefinition();
    if (def==null){
      Abort("invokation of undefined method.");
    }
    boolean inline;
    inline = inline(def);
    if (inline){
      int N=def.getArity();
      HashMap<NameExpression,ASTNode> map=new HashMap<NameExpression, ASTNode>();
      Substitution sigma=new Substitution(source(),map);
      map.put(create.reserved_name(ASTReserved.This), rewrite(e.object));
      for(int i=0;i<N;i++){
        map.put(create.unresolved_name(def.getArgument(i)),rewrite(e.getArg(i)));
      }
      result=sigma.rewrite(def.getBody());
    } else {
      super.visit(e);
    }
  }


  protected boolean inline(Method def) {
    boolean inline;
    if (def.isValidFlag(ASTFlags.INLINE)){
      inline=(def.kind==Method.Kind.Predicate || def.kind==Method.Kind.Pure) && def.getFlag(ASTFlags.INLINE);
    } else {
      inline=auto && def.kind==Method.Kind.Predicate && !def.isRecursive();
    }
    return inline;
  }

  @Override
  public void visit(Method m){
    if (inline(m)){
      result=null;
    } else {
      super.visit(m);
    }
  }
  
  @Override
  public void visit(OperatorExpression e){
    switch(e.getOperator()){
      case Unfolding:
      {
        ASTNode arg1=rewrite(e.getArg(0));
        ASTNode arg2=rewrite(e.getArg(1));
        if (arg1 instanceof MethodInvokation || arg1.isa(StandardOperator.Scale)){
          result=create.expression(StandardOperator.Unfolding,arg1,arg2);
        } else {
          result=arg2;
        }
        break;
      }
      case Unfold:
      case Fold:
      { 
        ASTNode arg=rewrite(e.getArg(0));
        if (arg instanceof MethodInvokation || arg.isa(StandardOperator.Scale)){
          result=create.expression(e.getOperator(),arg);
        } else {
          result=null; // returning null for a statement means already inserted or omit.
        }
        break;
      }
      default:
        super.visit(e);
        break;
    }
  }
}
