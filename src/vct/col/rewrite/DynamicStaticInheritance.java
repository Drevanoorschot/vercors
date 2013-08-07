package vct.col.rewrite;

import java.util.Arrays;

import vct.col.ast.ASTClass;
import vct.col.ast.ASTFlags;
import vct.col.ast.ASTNode;
import vct.col.ast.BlockStatement;
import vct.col.ast.ClassType;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.NameExpression;
import vct.col.ast.OperatorExpression;
import vct.col.ast.PrimitiveType.Sort;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;
import vct.col.ast.Type;
import static vct.col.ast.ASTReserved.*;

/**
 * Rewrites a program that uses inheritance into a program that does not
 * use inheritance any more, such that if the result verifies then the
 * original is correct.
 * 
 * Known limitations:
 * <UL>
 * <LI> No support for interfaces or multiple inheritance.
 * <LI> (Pure) method contracts with requires and/or ensures formulas that are not a single predicate invokation.
 * <li> no support for closed and/or final methods yet.
 * <li> No automatic inheritance for non-void methods.
 * </UL>
 * 
 * @author Stefan Blom
 *
 */
public class DynamicStaticInheritance extends AbstractRewriter {
  
  private static final String AT_STRING = "_at_";
  private AbstractRewriter copy_abstract=new AbstractRewriter(this){
    @Override
    public void visit(Method m){
      result=create.method_kind(m.kind,rewrite(m.getReturnType()), rewrite(m.getContract()), m.getName(), rewrite(m.getArgs()) , null);
    }
  };

  public static boolean isThis(ASTNode n){
    if (!(n instanceof NameExpression)) return false;
    NameExpression name=(NameExpression)n;
    if (name.getKind()!=NameExpression.Kind.Reserved) return false; 
    return name.getName().equals("this");
  }
  
  public static boolean isSuper(ASTNode n){
    if (!(n instanceof NameExpression)) return false;
    NameExpression name=(NameExpression)n;
    if (name.getKind()!=NameExpression.Kind.Reserved) return false; 
    return name.getName().equals("super");
  }

  private AbstractRewriter tag_this=new AbstractRewriter(this){
    public void visit(MethodInvokation e){
//      if (isThis(e.object)&&(e.getDefinition()==null||e.getDefinition().getKind()==Method.Kind.Predicate)){
      if (isThis(e.object)){
        String class_name=this.current_class().getName();
        result=create.invokation(rewrite(e.object), rewrite(e.dispatch), e.method+AT_STRING+class_name, rewrite(e.getArgs()));
      } else if (isSuper(e.object)){
        String class_name=this.current_class().super_classes[0].getName();
        result=create.invokation(create.reserved_name(This), rewrite(e.dispatch), e.method+AT_STRING+class_name, rewrite(e.getArgs()));
      } else {
        super.visit(e);
      }
    }
  };
  
  private AbstractRewriter fix_super=new AbstractRewriter(this){
    public void visit(MethodInvokation e){
      if (isSuper(e.object)){
        String class_name=this.current_class().super_classes[0].getName();
        result=create.invokation(create.reserved_name(This), rewrite(e.dispatch), e.method+AT_STRING+class_name, rewrite(e.getArgs()));
      } else {
        super.visit(e);
      }
    }
    public void visit(OperatorExpression e){
      switch(e.getOperator()){
        case Fold:
        case Unfold:
          result=split_predicates.rewrite(e);
          break;
        case Open:{
          MethodInvokation i=(MethodInvokation)e.getArg(0);
          MethodInvokation res=create.invokation(
              rewrite(i.object),
              null,
              "open_"+i.method+AT_STRING+i.dispatch.getFullName(),
              rewrite(i.getArgs())
            );
          res.set_before(rewrite(e.get_before()));
          res.set_after(rewrite(e.get_after()));
          result=res;
          break;
        }
        default:
          super.visit(e);
          break;
      }
    }
    public void visit(ClassType t){
      ASTClass cl=source().find(t.getNameFull());
      if (cl==null) {
        Method m=source().find_predicate(t.getNameFull());
        if (m==null){
          String name[]=t.getNameFull();
          String new_name[]=new String[name.length-1];
          for(int i=0;i<new_name.length-1;i++){
            new_name[i]=name[i];
          }
          int k=new_name.length-1;
          new_name[k]=name[k]+AT_STRING+name[k+1];
          result=create.class_type(new_name);
        } else {
          super.visit(t);
        }
      } else {
        super.visit(t);
      }
    }
  };
  
  private AbstractRewriter split_predicates=new AbstractRewriter(this){
    public void visit(MethodInvokation e){
      if (e.dispatch!=null){
        result=create.invokation(
          rewrite(e.object),
          null,
          e.method+AT_STRING+e.dispatch.getFullName(),
          rewrite(e.getArgs())
        );
      } else {
        super.visit(e);
      }
    }
  };
  
  private AbstractRewriter fix_super_plus=new AbstractRewriter(this){
    public void visit(MethodInvokation e){
      if (isSuper(e.object)){
        String class_name=this.current_class().super_classes[0].getName();
        result=create.invokation(create.reserved_name(This), rewrite(e.dispatch), e.method+AT_STRING+class_name, rewrite(e.getArgs()));
      } else {
        super.visit(e);
      }
    }
    public void visit(OperatorExpression e){
      switch(e.getOperator()){
        case Fold:
        case Unfold:
          result=split_predicates.rewrite(e);
          break;
        case Open:{
          MethodInvokation i=(MethodInvokation)e.getArg(0);
          MethodInvokation res=create.invokation(
              rewrite(i.object),
              null,
              "open_"+i.method+AT_STRING+i.dispatch.getFullName(),
              rewrite(i.getArgs())
            );
          BlockStatement tmp=rewrite(e.get_before());
          if (tmp==null) tmp=create.block();
          if (i.labels()>0){
            NameExpression label=i.getLabel(0);
            tmp.add_statement(create.assignment(create.label("family"),rewrite(label)));
          }
          res.set_before(tmp);
          res.set_after(rewrite(e.get_after()));
          result=res;
          break;
        }
        default:
          super.visit(e);
          break;
      }
    }
    public void visit(ClassType t){
      ASTClass cl=source().find(t.getNameFull());
      if (cl==null) {
        Method m=source().find_predicate(t.getNameFull());
        if (m==null){
          String name[]=t.getNameFull();
          String new_name[]=new String[name.length-1];
          for(int i=0;i<new_name.length-1;i++){
            new_name[i]=name[i];
          }
          int k=new_name.length-1;
          new_name[k]=name[k]+AT_STRING+name[k+1];
          result=create.class_type(new_name);
        } else {
          super.visit(t);
        }
      } else {
        super.visit(t);
      }
    }
  };
  
  public DynamicStaticInheritance(ProgramUnit source) {
    super(source);
  }
  
  public void visit(ASTClass cl){
    if (cl.implemented_classes.length>0) Fail("no support for interfaces");
    if (cl.super_classes.length>1) Fail("no support for multiple inheritance");
    ASTClass parent=null;
    if (cl.super_classes.length==1){
      parent=target().find(cl.super_classes[0]);
    }
    String class_name=cl.getName();
    ASTClass res=create.ast_class(class_name,cl.kind,new ClassType[0],new ClassType[0]);
    //should be function, but chalice messes up during printing.
    //res.add_dynamic(create.method_kind(Method.Kind.Pure,create.primitive_type(Sort.Boolean),null,"is_a_"+class_name,new DeclarationStatement[0],null));
    res.add_dynamic(create.predicate("is_a_"+class_name, null));
    res.add_dynamic(create.predicate("instance_of_"+class_name, null));
    for(DeclarationStatement decl:cl.dynamicFields()){
      res.add_dynamic(decl.apply(copy_rw));
    }
    if (parent!=null){
      for(DeclarationStatement decl:parent.dynamicFields()){
        res.add_dynamic(decl.apply(copy_rw));
      }
    }
    if (parent==null){
      for(Method m:cl.dynamicMethods()){
        // no abstract constructors!
        if (m.kind==Method.Kind.Constructor) continue;
        // all other kinds have abstract counterparts.
        res.add_dynamic(copy_abstract.rewrite(m));
      }
    } else {
      for(Method m:parent.dynamicMethods()){
        if (m.kind==Method.Kind.Predicate){
          res.add_dynamic(copy_rw.rewrite(m));
        } else {
          res.add_dynamic(copy_abstract.rewrite(m));
        }
        if (m.kind==Method.Kind.Constructor) continue;
        String name=m.getName();
        if (!name.contains(AT_STRING)){
          Type types[]=m.getArgType();
          int N=m.getArity();
          ASTNode names[]=new ASTNode[N];
          for(int i=0;i<N;i++){
            names[i]=create.local_name(m.getArgument(i));
          }
          if (cl.find(name,null,types,false)==null){
             Debug("%s inherits %s",cl.getName(),name);
             if (name.startsWith("is_a_")||name.startsWith("instance_of_")) continue;
             if (m.isValidFlag(ASTFlags.FINAL)&& m.getFlag(ASTFlags.FINAL)) continue;
             gen_inherited(res, parent, m, names, class_name);
           } else {
             Debug("%s overrides %s",cl.getName(),name);
           }
        }
      }
    }
    for(Method m:cl.dynamicMethods()){
      boolean is_final=m.isValidFlag(ASTFlags.FINAL)&&m.getFlag(ASTFlags.FINAL);
      switch(m.kind){
      case Predicate:{
        int N=m.getArity();
        ASTNode names[]=new ASTNode[N];
        for(int i=0;i<N;i++){
          names[i]=create.local_name(m.getArgument(i));
        }
        ContractBuilder cb=new ContractBuilder();
        cb.requires(create.invokation(null,null,m.getName(),names).labeled("family"));
        cb.requires(create.invokation(null,null,"is_a_"+class_name).labeled("class_of"));
        cb.ensures(create.invokation(null,null,m.getName()+AT_STRING+class_name,names).labeled("member"));
        Method open=create.method_decl(
            create.primitive_type(Sort.Void),
            cb.getContract(),
            "open_"+m.getName()+AT_STRING+class_name,
            rewrite(m.getArgs()),
            null
        );
        res.add_dynamic(open);
        ASTNode body=tag_this.rewrite(m.getBody());
        if (parent!=null){
          for(int i=0;i<N;i++){
            names[i]=create.local_name(m.getArgument(i));
          }
          ASTNode base=create.invokation(
              create.reserved_name(This),
              null,
              m.getName()+AT_STRING+parent.getName(),
              names);
          base.labeled("parent");
          body=create.expression(StandardOperator.Star,base,body);
        }
        Method local=create.method_kind(
            m.kind,
            copy_rw.rewrite(m.getReturnType()),
            null,
            m.getName()+AT_STRING+class_name,
            copy_rw.rewrite(m.getArgs()),
            body);
        res.add_dynamic(local);
        break;
      }
      case Pure:
      case Plain:{
        Contract c=m.getContract();
        if (parent!=null){
          Method override=parent.find(m.getName(),null,m.getArgType());
          if (c!=null && override!=null){
            Fail("alternate contracts are not supported at %s",m.getOrigin());
          }
          if (override!=null){
            c=override.getContract();
          }
        }
        Method local=create.method_kind(
            m.kind,
            copy_rw.rewrite(m.getReturnType()),
            (is_final?copy_rw:tag_this).rewrite(c),
            m.getName()+AT_STRING+class_name,
            copy_rw.rewrite(m.getArgs()),
            (is_final?fix_super:fix_super_plus).rewrite(m.getBody()));
        res.add_dynamic(local);
        break;
      }
      case Constructor:{
        ContractBuilder cb=new ContractBuilder();
        Contract c=m.getContract();
        if (c!=null) copy_rw.rewrite(c,cb);
        cb.ensures(create.invokation(create.reserved_name(This),null,"is_a_"+class_name).labeled("class_of"));
        Method global=create.method_kind(
            m.kind,
            copy_rw.rewrite(m.getReturnType()),
            cb.getContract(),
            m.getName(),
            copy_rw.rewrite(m.getArgs()),
            null);
        res.add_dynamic(global);
        Method local=create.method_kind(
            m.kind,
            copy_rw.rewrite(m.getReturnType()),
            tag_this.rewrite(m.getContract()),
            m.getName()+AT_STRING+class_name,
            copy_rw.rewrite(m.getArgs()),
            fix_super.rewrite(m.getBody()));
        res.add_dynamic(local);
        break;
      }
      default:
        Fail("missing case: %s",m.kind);
      }
    }
    result=res;
  }

  private void gen_inherited(ASTClass res,ASTClass parent,Method m,ASTNode names[],String class_name) {
    String name=m.getName();
    Contract c=m.getContract();
    BlockStatement block=create.block();
    ASTNode body=block;
    switch(m.kind){
    case Pure:{
      block.add_statement(create.expression(StandardOperator.Unfold,tag_this.rewrite(c.pre_condition)));
      block.add_statement(create.return_statement(create.invokation(
          create.reserved_name(This),null,name+AT_STRING+parent.getName(),names)));
      break;
    }
    case Plain:{
      Type t=m.getReturnType();
      block.add_statement(create.expression(StandardOperator.Unfold,tag_this.rewrite(c.pre_condition)));
      if (t.isVoid()){
        block.add_statement(create.invokation(
            create.reserved_name(This),null,name+AT_STRING+parent.getName(),names));
      } else {
        Abort("unsupported non-void method");
      }
      block.add_statement(create.expression(StandardOperator.Fold,tag_this.rewrite(c.post_condition)));
      break;
    }
    case Predicate:{
      body=create.invokation(
          create.reserved_name(This),
          null,
          m.getName()+AT_STRING+parent.getName(),
          names);
      body.labeled("parent");
      break;
    }
    default:
      Abort("missing case: %s",m.kind);
    }
    Method local=create.method_kind(
        m.kind,
        copy_rw.rewrite(m.getReturnType()),
        tag_this.rewrite(c),
        m.getName()+AT_STRING+class_name,
        copy_rw.rewrite(m.getArgs()),
        body);
    res.add_dynamic(local);             

  }
  

}
