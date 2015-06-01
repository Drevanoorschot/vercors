package vct.col.rewrite;

import static hre.System.*;

import java.util.HashMap;

import vct.col.ast.ASTFlags;
import vct.col.ast.ASTNode;
import vct.col.ast.ASTReserved;
import vct.col.ast.AssignmentStatement;
import vct.col.ast.BlockStatement;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.NameExpression;
import vct.col.ast.PrimitiveType;
import vct.col.ast.ProgramUnit;
import vct.col.ast.ReturnStatement;
import vct.col.ast.StandardOperator;

public class VoidCalls extends AbstractRewriter {

  public VoidCalls(ProgramUnit source) {
    super(source);
  }
  
  /* TODO: we have a serious order bug, where
   * statements about result are made before result is assigned.
   */
  public void visit(NameExpression e){
    if (e.isReserved(ASTReserved.Result)){
      result=create.unresolved_name("sys__result");
    } else {
      super.visit(e);
    }
  }
  
  public void visit(Method m){
    switch(m.kind){
    case Predicate:
    case Pure:
      result=copy_rw.rewrite(m);
      return;
    default:
      break;
    }   
    if (m.getReturnType().isVoid()){
      super.visit(m);
    } else {
      DeclarationStatement m_args[]=m.getArgs();
      int N=m_args.length;
      DeclarationStatement args[]=new DeclarationStatement[N+1];
      args[0]=new DeclarationStatement("sys__result",rewrite(m.getReturnType()));
      args[0].setOrigin(m);
      args[0].setFlag(ASTFlags.OUT_ARG, true);
      for(int i=0;i<N;i++){
        args[i+1]=rewrite(m_args[i]);
      }
      result=create.method_decl(
          create.primitive_type(PrimitiveType.Sort.Void),
          rewrite(m.getContract()),
          m.getName(),
          args,
          rewrite(m.getBody()));
    }
  }
  
  public void visit(ReturnStatement s){
    ASTNode expr=s.getExpression();
    BlockStatement res=create.block();
    if (expr!=null){
      res.add(create.assignment(create.local_name("sys__result"),rewrite(expr)));
    }
      for(ASTNode n : s.get_after()){
        res.add(rewrite(n));
      }
      if (current_method().getContract()!=null){
        res.add(create.expression(StandardOperator.Assert,rewrite(current_method().getContract().post_condition)));
      }
      res.add(create.expression(StandardOperator.Assume,create.constant(false)));
      result=res;
//    } else {
//      super.visit(s);
//    }
  }
  
  public void visit(MethodInvokation e){
    Method m=e.getDefinition();
    if (m==null) Abort("unexpected null method definition at %s",e.getOrigin());
    switch(m.kind){
    case Predicate:
    case Pure:
      super.visit(e);
      return;
    default:
      break;
    }
    if (!m.getReturnType().isVoid()){
      Fail("unexpected non-void method invokation at %s",e.getOrigin());
    }
    super.visit(e);
  }
  
  public void visit(AssignmentStatement s){
    if (s.getExpression() instanceof MethodInvokation){
      MethodInvokation e=(MethodInvokation)s.getExpression();
      Method m=e.getDefinition();
      if (e==null) Abort("cannot process invokation of %s without definition",e.method);
      if (m.kind==Method.Kind.Plain){
        int N=e.getArity();
        ASTNode args[]=new ASTNode[N+1];
        args[0]=rewrite(s.getLocation());
        for(int i=0;i<N;i++){
          args[i+1]=rewrite(e.getArg(i));
        }
        args[0]=rewrite(s.getLocation());
        MethodInvokation res=create.invokation(rewrite(e.object), rewrite(e.dispatch) , e.method , args );
        for(NameExpression lbl:e.getLabels()){
          Debug("VOIDCALLS: copying label %s",lbl);
          res.addLabel(rewrite(lbl));
        }
        res.set_before(rewrite(e.get_before()));
        HashMap<NameExpression,ASTNode>map=new HashMap<NameExpression,ASTNode>();
        map.put(create.reserved_name(ASTReserved.Result),rewrite(s.getLocation()));
        Substitution subst=new Substitution(source(),map);
        res.set_after(subst.rewrite(e.get_after()));
        result=res;
        return;
      }
    }
    super.visit(s);
  }
}
