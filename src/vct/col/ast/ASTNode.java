// -*- tab-width:2 ; indent-tabs-mode:nil -*-
package vct.col.ast;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import vct.col.ast.ASTSpecial.Kind;

import hre.ast.Origin;
import static hre.System.Abort;
import static hre.System.Debug;
import static hre.System.Fail;
import static hre.System.Warning;

/** common features of all AST nodes. */
public abstract class ASTNode implements ASTFlags {

  public ASTNode labeled(String name){
    NameExpression label=new NameExpression(NameExpression.Kind.Label,null,name);
    label.setOrigin(this.origin);
    addLabel(label);
    return this;
  }
  
  /**
   * list of annotations.
   */
  private ArrayList<ASTNode> annotations;
  
  /**
   * Contains the labels used to mark this node.
   */
  private ArrayList<NameExpression> labelset=new ArrayList<NameExpression>();
  
  /**
   * Contains the set of flags set for this node as a bitset.
   */
  private int flags=0;
  /**
   * Contains the valid flags for this node as a bitset.
   */
  private int valid_flags=0;
  
  /**
   * This variable works around the problem that PVL and Java have different
   * constructor definitions.
   */
  public static boolean pvl_mode=false;

  /**
   * Auxiliary variable to help assign unique sequence numbers for AST nodes.
   */
  private static long next_id=0;

  public void copyMissingFlags(ASTNode node){
    int missing_flags=node.valid_flags & (~valid_flags);
    valid_flags=valid_flags|missing_flags;
    flags=flags|(node.flags&missing_flags);
  }

  public boolean getFlag(int flag){
    if ((valid_flags & flag)!=flag) Abort("flag %d has not been set",flag);
    return (flags & flag)!=0 ;    
  }

  public boolean isStatic(){
    return getFlag(STATIC);
  }

  public boolean isValidFlag(int flag){
    return (valid_flags&flag)==flag;
  }
  
  public void setStatic(boolean val){
    setFlag(STATIC,val);
  }
  
  public void setFlag(int flag,boolean val){
    valid_flags |= flag;
    if (val) {
      flags |= flag;
    } else {
      flags &= ~flag;
    }
  }

  public boolean isGhost(){
    if ((valid_flags & GHOST)==0) Abort("ghost flag has not been set");
    return (flags & GHOST)!=0 ;
  }

  public void setGhost(boolean val){
    valid_flags |= GHOST;
    if (val) {
      flags |= GHOST;
    } else {
      flags &= ~GHOST;
    }
  }
  
  private long id;

  public ASTNode() {
    synchronized(ASTNode.class/*this.getClass()*/){
      this.id=next_id;
      ++next_id;
    }
  }

  private Origin origin;
  
  public void setOrigin(ASTNode node){
    setOrigin(node.origin);
  }
  
  public void setOrigin(Origin origin){
    if (origin==null) throw new Error("illegal null origin");
    if (this.origin!=null) throw new Error("origin already set");
    this.origin=origin;
  }
  
  public Origin getOrigin(){
    return origin;
  }

  public void clearOrigin(){
    if (origin==null) Abort("clearing null origin.");
    origin=null;
  }

  protected abstract <T> void accept_simple(ASTVisitor<T> visitor);
  
  public final <T> void accept(ASTVisitor<T> visitor){
    visitor.pre_visit(this);
    this.accept_simple(visitor);
    visitor.post_visit(this);
  }
  
  public final <T> T apply(ASTVisitor<T> visitor){
    this.accept(visitor);
    return visitor.getResult();
  }

  public long getUniqueID(){
    return id;
  }
  
  private Type t=null;

  public Type getType() {
    return t;
  }
  public void setType(Type t){
    this.t=t;
  }
  
  private ASTNode parent;
  
  public ASTNode getParent(){
    return parent;
  }

  public void setParent(ASTNode parent){
    if (parent==null){
      throw new Error("illegal null parent");
    }
    if (this.parent==parent){
      Warning("setting the same parent twice");
    }
    if (this.parent!=null){
      Warning("modifying parent of %s from %s",this.getClass(),this.getOrigin());
      Warning("original parent is %s",this.getParent().getOrigin());
      Warning("new parent is %s",parent.getOrigin());
      Abort("modifying parent of %s from %s",this.getClass(),this.getOrigin());
    }
    this.parent=parent;
  }
  
  public final void accept_if(ASTVisitor v){
    if (v!=null) accept(v);
  }
  
  public ASTClass getClass(Type t){
    if (t instanceof ClassType) {
      return getClass((ClassType)t);
    } else {
      Fail("not a class type");
      return null;
    }
  }

  public ASTClass getClass(ClassType t){
    ASTNode tmp=this;
    while(tmp.getParent()!=null){
      tmp=tmp.getParent();
    }
    ASTClass root=(ASTClass)tmp;
    return root.find(t.getNameFull());
  }
  
  /**
   * Add a label to this node.
   * 
   * Examples of labels are 
   * <ul>
   *   <li>Statement labels used for jumps.</li>
   *   <li>Predicate labels.</li>
   * </ul>
   */
  public void addLabel(NameExpression label){
    if (label.getKind()!=NameExpression.Kind.Label) {
      Abort("cannot label with %s of kind %s",label,label.getKind());
    }
    labelset.add(label);
  }
  
  /**
   * Allow iteration over the set of labels.
   */
  public Iterable<NameExpression> getLabels(){
    return labelset;
  }

  public NameExpression getLabel(int i){
    return labelset.get(i);
  }
  public int labels() {
    return labelset.size();
  }

  /**
   * Check if this node is an operator expression.
   * @param op operator
   * @return true is this AST is an <code>op</code>-expression, false otherwise.
   */
  public boolean isa(StandardOperator op) {
    return false;
  }

  public boolean isName(String name) {
    return false;
  }
  
  private HashSet<ASTNode> predecessors;
  
  public void setPredecessor(ASTNode item){
    if (predecessors!=null){
      Abort("predecessors is not null");
    }
    predecessors=new HashSet<ASTNode>();
    predecessors.add(item);
  }
  
  public void setPredecessor(Set<ASTNode> init){
    if (predecessors!=null){
      Abort("predecessors is not null");
    }
    predecessors=new HashSet<ASTNode>(init);
  }

  public void addPredecessor(ASTNode item){
    if (predecessors==null){
      Abort("predecessors is null");
    }
    predecessors.add(item);
  }
  public void addPredecessor(Set<ASTNode> items){
    if (predecessors==null){
      Abort("predecessors is null");
    }
    predecessors.addAll(items);
  }

  public Set<ASTNode> getPredecessors(){
    return predecessors;
  }

  public void attach(ASTNode ... annotation_list) {
    if (annotations==null){
      annotations=new ArrayList<ASTNode>();
    }
    for (ASTNode annotation : annotation_list){
      if (annotation instanceof NameExpression){
        NameExpression name=(NameExpression)annotation;
        if (name.reserved()!=null){
          switch(name.reserved()){
            case Static:
              setStatic(true);
              continue;
            case Public:
              Debug("ignoring public");
              continue;
          }
        }
      }
      annotations.add(annotation);
    }
  }

  public Iterable<ASTNode> annotations(){
    return annotations;
  }
  
  public boolean annotated(){
    return annotations!=null; 
  }

  public boolean isReserved(ASTReserved any) {
    return false;
  }

  public void clearParent() {
    parent=null;
  }

  public boolean isSpecial(Kind with) {
    return false;
  }

}
