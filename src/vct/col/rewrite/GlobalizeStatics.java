package vct.col.rewrite;

import hre.ast.MessageOrigin;
import vct.col.ast.ASTClass;
import vct.col.ast.ASTClass.ClassKind;
import vct.col.ast.ASTNode;
import vct.col.ast.ClassType;
import vct.col.ast.CompilationUnit;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.Dereference;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.NameExpression;
import vct.col.ast.OperatorExpression;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;
import vct.col.ast.Type;
import vct.util.ClassName;
import static hre.System.Abort;
import static hre.System.Debug;
import static hre.System.Fail;
import static hre.System.Warning;

/**
 * Base class for rewriting all static entries as a single Global class.
 * This base class will do all of the rewriting, except the creation
 * of the name global that refers to the global entries. The class
 * {@link GlobalizeStaticsParameter} adds a parameter global to all
 * non-static methods. The class {@link GlobalizeStaticsField} adds
 * a field global to every class.
 * 
 * An advantage of adding a field is that it allows non-static predicates
 * to refer to static variables without adding an argument.
 * A disadvantage is that it requires generating contracts to make it work.
 * 
 * @author sccblom
 *
 */
public abstract class GlobalizeStatics extends AbstractRewriter {
  
  public GlobalizeStatics(ProgramUnit source) {
    super(source);
    global_class=create(new MessageOrigin("filtered globals")).ast_class("Global",ClassKind.Plain,null,null);
    target().add(new CompilationUnit("Globals").add(global_class));
  }

  protected ASTClass global_class;
  protected String prefix;
  protected boolean processing_static;
  
  public void visit(ASTClass cl){
    switch(cl.kind){
    case Plain:{
      int N;
      ASTClass res=create.ast_class(cl.name,ClassKind.Plain,null,null);
      N=cl.getStaticCount();
      prefix=new ClassName(cl.getFullName()).toString("_");
      processing_static=true;
      Debug("prefix is now %s",prefix);
      for(int i=0;i<N;i++){
        global_class.add_dynamic(cl.getStatic(i).apply(this));
      }
      prefix=null;
      processing_static=false;
      Debug("prefix is now %s",prefix);
      N=cl.getDynamicCount();
      for(int i=0;i<N;i++){
        res.add_dynamic(cl.getDynamic(i).apply(this));
      }
      result=res;
      break;      
    }
    default: Abort("missing case");
    }
  }
  public void visit(DeclarationStatement s){
    if (prefix!=null){
      String save=prefix;
      prefix=null;
      result=create.field_decl(save+"_"+s.getName(),
           rewrite(s.getType()), 
           rewrite(s.getInit()));
      prefix=save;
    } else {
      super.visit(s);
    }
  }
  public void visit(Method m){
    if (prefix!=null){
      String save=prefix;
      prefix=null;
      result=create.method_decl(
          rewrite(m.getReturnType()),
          rewrite(m.getContract()),
          save+"_"+m.getName(),
          rewrite(m.getArgs()),
          rewrite(m.getBody()));
      prefix=save;
    } else {
      super.visit(m);
    }
  }

  public void visit(MethodInvokation e){
    Method m=e.getDefinition();
    if (m==null) Abort("cannot globalize method invokaiton without method definition");
    ASTClass cl=(ASTClass)m.getParent();
    if (m.isStatic() && !e.isInstantiation()){
      String prefix=new ClassName(cl.getFullName()).toString("_");
      if (processing_static){
        result=create.invokation(
          create.this_expression(create.class_type("Global")),
          rewrite(e.dispatch),
          prefix+"_"+e.method,
          rewrite(e.getArgs()));
      } else {
        result=create.invokation(
            create.local_name("global"),
            rewrite(e.dispatch),
            prefix+"_"+e.method,
            rewrite(e.getArgs()));        
      }
    } else {
      super.visit(e);
    }
  }

  public void visit(Dereference e){
    if (e.field.equals("length")){
      super.visit(e);
      return;
    }
    ClassType t;
    if (e.object instanceof ClassType){
      t=(ClassType)e.object;
    } else {
      t=(ClassType)e.object.getType();
    }
    ASTClass cl=source().find(t);
    DeclarationStatement decl=cl.find_field(e.field);
    if (decl.isStatic()){
      String var_name=new ClassName(t.getNameFull()).toString("_")+"_"+e.field;
      if (!processing_static){
        result=create.dereference(create.local_name("global"),var_name);
      } else {
        result=create.dereference(create.this_expression(create.class_type("Global")),var_name);
      }     
    } else {
      super.visit(e);
    }
  }

}
