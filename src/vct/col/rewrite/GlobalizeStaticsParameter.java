package vct.col.rewrite;

import hre.ast.MessageOrigin;
import vct.col.ast.ASTClass;
import vct.col.ast.ASTClass.ClassKind;
import vct.col.ast.ASTNode;
import vct.col.ast.ASTReserved;
import vct.col.ast.ClassType;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.LoopStatement;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.NameExpression;
import vct.col.ast.OperatorExpression;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;
import vct.util.ClassName;
import static hre.System.Abort;
import static hre.System.Debug;
import static hre.System.Fail;
import static hre.System.Warning;

/**
 * Use a parameter global to refer to static entries.
 * 
 * @author sccblom
 *
 */
public class GlobalizeStaticsParameter extends GlobalizeStatics {

  public GlobalizeStaticsParameter(ProgramUnit source) {
    super(source);
  }

  /**
   * Add the global argument to every non-static method.
   */
  public void visit(Method m){
    if (prefix!=null){
      super.visit(m);
    } else {
      switch(m.getKind()){
      case Constructor:
      case Plain: {
        DeclarationStatement args[]=new DeclarationStatement[m.getArity()+1];
        //TODO: parameter decl in factory!
        args[0]=create.field_decl("global",create.class_type("Global"));
        for(int i=1;i<args.length;i++){
          args[i]=rewrite(m.getArgs()[i-1]);
        }
        result=create.method_kind(
            m.getKind(),
            rewrite(m.getReturnType()),
            rewrite(m.getContract()),
            m.getName(),
            args,
            rewrite(m.getBody()));
        break;
      }
      default:
        super.visit(m);
      }
    }
  }
  
  /**
   * Add the this/global argument to every invokation of a non-static method.
   */
  public void visit(MethodInvokation e){
    Method m=e.getDefinition();
    if (m==null) Abort("cannot globalize method invokaiton without method definition");
    ASTClass cl=(ASTClass)m.getParent();
    if (m.isStatic() && !e.isInstantiation()){
      super.visit(e);
    } else {
      Method.Kind kind=Method.Kind.Predicate;
      if (e.getDefinition()!=null){
        kind=e.getDefinition().getKind();
      } else {
        Warning("assuming kind of %s is Predicate",e.method);
      }
      switch(kind){
      case Constructor:
      case Plain:{
        ASTNode args[]=new ASTNode[e.getArity()+1];
        if (processing_static){
          args[0]=create.reserved_name(ASTReserved.This);
        } else {
          args[0]=create.local_name("global");
        }
        for(int i=1;i<args.length;i++){
          args[i]=rewrite(e.getArg(i-1));
        }
        MethodInvokation res=create.invokation(
            rewrite(e.object),
            rewrite(e.dispatch),
            e.method,
            args
        );
        if (e.get_before().size()>0) {
          res.set_before(rewrite(e.get_before()));
        }
        if (e.get_after().size()>0) {
          res.set_after(rewrite(e.get_after()));
        }
        result=res;
        break;
      }
      case Pure:
      case Predicate:
        super.visit(e);
        break;
      default:
        Abort("missing case: %s",kind);
        break;
      }
    }
  }
  
  @Override
  public void visit(ASTClass cl){
    if (cl.kind==ASTClass.ClassKind.Kernel){
      result=copy_rw.rewrite(cl);
    } else {
      super.visit(cl);
    }
  }
}

