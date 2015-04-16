package vct.silver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hre.HREError;
import hre.ast.Origin;
import vct.col.ast.*;
import vct.col.ast.PrimitiveType.Sort;
import vct.util.Configuration;
import static hre.System.Abort;

public class SilverExpressionMap<T,E,Decl> implements ASTMapping<E>{

  private SilverVerifier<Origin,?,T,E,?,Decl,?,?,?> create;
  private SilverTypeMap<T> type;

  public SilverExpressionMap(SilverVerifier<Origin,?,T,E,?,Decl,?,?,?> backend,SilverTypeMap<T> type){
    this.create=backend;
    this.type=type;
  }

  @Override
  public void pre_map(ASTNode n) {
  }

  @Override
  public E post_map(ASTNode n, E res) {
    if (res==null){
      Origin o=n.getOrigin();
      throw new HREError("cannot map %s to expression (%s)",n.getClass(),o!=null?o:"without origin");
    }
    return res;
  }

  @Override
  public E map(StandardProcedure p) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ConstantExpression e) {
    if (e.value instanceof IntegerValue){
      return create.Constant(e.getOrigin(),((IntegerValue)e.value).value);
    } else if (e.value instanceof BooleanValue) {
      return create.Constant(e.getOrigin(),((BooleanValue)e.value).value);
    } else {
      throw new HREError("cannot map constant value %s",e.value.getClass());
    }
  }

  @Override
  public E map(OperatorExpression e) {
    Origin o = e.getOrigin();
    switch(e.getOperator()){
    case Build:{
      ASTNode args[]=e.getArguments();
      ArrayList<E> elems=new ArrayList<E>();
      for(int i=1;i<args.length;i++){
        elems.add(args[i].apply(this));
      }
      T t=((Type)((Type)e.getArg(0)).getArg(0)).apply(type);
      switch(((PrimitiveType)args[0]).sort){
      case Sequence:
        return elems.size()>0?create.explicit_seq(o, elems):create.empty_seq(o,t);
      case Bag:
        return elems.size()>0?create.explicit_bag(o, elems):create.empty_bag(o,t);
      case Set:
        return elems.size()>0?create.explicit_set(o, elems):create.empty_set(o,t);
      default:
        return null;
      }
    }
    case Nil:{
      //T t=((Type)((Type)e.getArg(0)).getArg(0)).apply(type);
      T t=((Type)e.getArg(0)).apply(type);
      return create.empty_seq(o,t);
    }
    case PointsTo:{
      E e1=e.getArg(0).apply(this);
      E e2=e.getArg(1).apply(this);
      E e3=e.getArg(2).apply(this);
      return create.and(o,create.field_access(o,e1,e2),create.eq(o, e1, e3));
    }
    case ITE: return create.cond(o,e.getArg(0).apply(this),e.getArg(1).apply(this),e.getArg(2).apply(this));
    case Perm: return create.field_access(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Value: return create.field_access(o,e.getArg(0).apply(this),create.read_perm(o));
    case Star: return create.and(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case And: return create.and(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Or: return create.or(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Implies: return create.implies(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Not: return create.not(o,e.getArg(0).apply(this));
    case Unfolding: return create.unfolding_in(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Old: return create.old(o,e.getArg(0).apply(this));
    
    case Size: return create.size(o,e.getArg(0).apply(this));
    case Tail: return create.drop(o,e.getArg(0).apply(this),create.Constant(o,1));
    case Member: {
      if (e.getArg(1).getType().isPrimitive(Sort.Sequence)){
        return create.seq_contains(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      } else {
        return create.any_set_contains(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      }
    }
    case RangeSeq: return create.range(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      
    case Subscript: return create.index(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    
    case GT: return create.gt(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case LT: return create.lt(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case GTE: return create.gte(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case LTE: return create.lte(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case EQ: return create.eq(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case NEQ: return create.neq(o,e.getArg(0).apply(this),e.getArg(1).apply(this));

    case Mult: return create.mult(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Div:{
      if (e.getType().isPrimitive(PrimitiveType.Sort.Fraction)){
        return create.frac(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      } else {
        return create.div(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      }
    }
    case Mod: return create.mod(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case Plus:{
      if (e.getType().isPrimitive(Sort.Sequence)){
        return create.append(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      } else if (e.getType().isPrimitive(Sort.Set) || e.getType().isPrimitive(Sort.Bag)){
        return create.union(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      } else {
        return create.add(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
      }
    }
    case Minus: return create.sub(o,e.getArg(0).apply(this),e.getArg(1).apply(this));
    case UMinus: return create.neg(o,e.getArg(0).apply(this));
    case Scale:{
      return create.scale_access(o,e.getArg(1).apply(this), e.getArg(0).apply(this));
    }
    default:
        throw new HREError("cannot map operator %s",e.getOperator());
    }
  }

  @Override
  public E map(NameExpression e) {
    Origin o = e.getOrigin();
    switch(e.getKind()){
    //case Label:
    //  hre.System.Warning("assuming label means local at %s",e.getOrigin());
    case Local:
    case Argument:
      return create.local_name(o,e.getName(),e.getType().apply(type));
    case Reserved:
      switch(e.reserved()){
      case Null:
        return create.null_(o);
      case FullPerm: return create.write_perm(o);
      case ReadPerm: return create.read_perm(o);
      case NoPerm: return create.no_perm(o);
      case Result: return create.result(o,e.getType().apply(type));
      }
      // fall through on purpose
      default:
        throw new HREError("cannot map %s name %s",e.getKind(),e.getName());
    }
  }

  @Override
  public E map(ClassType t) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(FunctionType t) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(PrimitiveType t) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(RecordType t) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(MethodInvokation e) {
    Method m=e.getDefinition();
    Origin o=e.getOrigin();
    String name=e.method;
    ArrayList<E> args=new ArrayList<E>();
    for(ASTNode n:e.getArgs()){
      args.add(n.apply(this));
    }
    switch(m.kind){
    case Pure:{
      T rt=m.getReturnType().apply(type);
      ArrayList<Decl> pars=new ArrayList<Decl>();
      for(DeclarationStatement decl:m.getArgs()){
        pars.add(create.decl(decl.getOrigin(),decl.getName(),decl.getType().apply(type)));
      }
      if(m.getParent() instanceof AxiomaticDataType){
        //TODO: use correct dpars map!
        HashMap<String, T> dpars=new HashMap<String, T>();
        return create.domain_call(o, name, args, dpars, rt, pars);
      } else {
        return create.function_call(o, name, args, rt, pars);
      }
    }
    case Predicate:{
      return create.predicate_call(o, name, args);
    }
    default:
      throw new HREError("calling a %d method is not a Silver expression");
    }
  }

  @Override
  public E map(BlockStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(IfStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ReturnStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(AssignmentStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(DeclarationStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(LoopStatement s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(Method m) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ASTClass c) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ASTWith astWith) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(BindingExpression e) {
    Origin o = e.getOrigin();
    switch(e.binder){
    case STAR:
    case FORALL:
      E expr;
      if (e.select.isConstant(true)){
        expr=e.main.apply(this);
      } else {
        expr=create.implies(o, e.select.apply(this), e.main.apply(this));
      }
      return create.forall(o, convert(e.getDeclarations()),expr);
    case EXISTS:
      return create.exists(o, convert(e.getDeclarations()),create.and(o, e.select.apply(this), e.main.apply(this)));
    default:
      Abort("binder %s not supported",e.binder);
    }
    return null;
  }

  private List<Decl> convert(DeclarationStatement[] declarations) {
    ArrayList<Decl> res=new ArrayList();
    for(DeclarationStatement d:declarations){
      res.add(create.decl(d.getOrigin(),d.getName(),d.getType().apply(type)));
    }
    return res;
  }

  @Override
  public E map(Dereference e) {
    return create.FieldAccess(e.getOrigin(),e.object.apply(this),e.field,e.getType().apply(type));
  }

  @Override
  public E map(Lemma lemma) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ParallelBarrier parallelBarrier) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ParallelBlock parallelBlock) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(Contract contract) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ASTSpecial special) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(VariableDeclaration variableDeclaration) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(TupleType tupleType) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public E map(TypeExpression t) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(AxiomaticDataType adt) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(Axiom axiom) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(Hole hole) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ActionBlock actionBlock) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ASTSpecialDeclaration s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ForEachLoop s) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(ParallelAtomic parallelAtomic) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(NameSpace nameSpace) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public E map(TryCatchBlock tcb) {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  

}
