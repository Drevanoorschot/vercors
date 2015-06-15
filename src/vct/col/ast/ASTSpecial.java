package vct.col.ast;

import java.util.Arrays;

import vct.util.ClassName;

public class ASTSpecial extends ASTNode {

  @Override
  public <R,A> R accept_simple(ASTMapping1<R,A> map,A arg){
    return map.map(this,arg);
  }

  public static enum Kind {
    Assert,
    Expression,
    // Invariant,
//    Fold
    With,
    Then,
    Proof,
    Import,
    Throw,
    Label,
    //Contract, Requires, Ensures, Given, Yields, Modifies,
    Exhale,
    Inhale,
    DeclareAction,
    CreateHistory,
    DestroyHistory,
    SplitHistory,
    MergeHistory,
    Transfer,
    Goto,
    SpecIgnoreStart,
    SpecIgnoreEnd,
    Lock,
    Unlock,
    Fork,
    Join
  };

  public final Kind kind;
  
  public final ASTNode[] args;
  
  public ASTSpecial(Kind kind,ASTNode ... args){
    this.kind=kind;
    this.args=Arrays.copyOf(args,args.length);
  }

  
  @Override
  public <T> void accept_simple(ASTVisitor<T> visitor){
    try {
      visitor.visit(this);
    } catch (Throwable t){
      if (thrown.get()!=t){
        System.err.printf("Triggered by %s:%n",getOrigin());
        thrown.set(t);
      }
      throw t;
    }
  }
  
  @Override
  public <T> T accept_simple(ASTMapping<T> map){
    try {
      return map.map(this);
    } catch (Throwable t){
      if (thrown.get()!=t){
        System.err.printf("Triggered by %s:%n",getOrigin());
        thrown.set(t);
      }
      throw t;
    }
  }
 
  
  public boolean isSpecial(Kind with) {
    return kind==with;
  }

}
