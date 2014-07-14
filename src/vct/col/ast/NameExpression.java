// -*- tab-width:2 ; indent-tabs-mode:nil -*-
package vct.col.ast;

import vct.col.ast.NameExpression.Kind;

/** Node that can hold every possible kind of defined name.
 * 
 * @author Stefan Blom
 *
 */
public class NameExpression extends ASTNode {

  /** The possible kinds of defined names */
  public static enum Kind {
    /** an unresolved name */
    Unresolved,
    /** argument to a function */
    Argument,
    /** local variable */
    Local,
    /** a field in a class */
    Field,
    /** a method in a class */
    Method,
    /** for the reserved names: null, this, and super. */
    Reserved,
    /** for labels, such as statement labels and predicate labels. */
    Label,
    /** for the ?x binder of VeriFast. */
    Output;
  }
  
  /** The name that this AST node is referencing. */
  private String name;
  /** The reserved name this node contains. */
  private ASTReserved reserved;
  /** The kind of the definition being referenced. */
  private Kind kind;
  /** The site where this name was defined. */
  private ASTNode site;
  
  public NameExpression(ASTReserved name){
    reserved=name;
    kind=Kind.Reserved;
    this.name=name.toString();
  }
  
  /** Create an unresolved name expression */
  public NameExpression(String name){
    this.name=name;
    kind=Kind.Unresolved;
  }
  /** Create an specific kind of name expression */
  public NameExpression(Kind kind,ASTReserved word,String name){
    this.name=name;
    this.reserved=word;
    this.kind=kind;
  }
  
  public void setKind(Kind kind){
    if (kind==Kind.Reserved) hre.System.Abort("cannot just declared a word reserved");
    this.kind=kind;
  }
  public Kind getKind(){
    return kind;
  }
  public ASTNode getSite(){
    return site;
  }
  public void setSite(ASTNode site){
    this.site=site;
  }
  public String getName() { return name; }

  
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
 
  public String toString(){ return name; }

  public boolean equals(Object o){
    if (o instanceof NameExpression){
      NameExpression other=(NameExpression)o;
      return name.equals(other.name);
    }
    return false;
  }
  
  public boolean isName(String name) {
    return this.name.equals(name);
  }

  public int hashCode(){
    return name.hashCode();
  }
  
  public boolean isReserved(ASTReserved word) {
    return reserved==word;
  }

  public ASTReserved reserved(){
    return reserved;
  }

  public boolean match(ASTNode ast){
    if (ast instanceof Hole){
      return ast.match(this);
    } else { 
      return equals(ast);
    }
  }
}

