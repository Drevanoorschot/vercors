package vct.col.ast;

public enum ASTReserved {
  /**
   * Specification expression that represents the result of a method or function in a post condition. 
   */
  Result,
  /**
   * Refers to the super class of the current object.
   */
  Super,
  /**
   * Refers to the current object.
   */
  This,
  /**
   * Denotes a public member of a class or module.
   */
  Public,
  /**
   * Denotes a private member of a class or module.
   * (For example the C keyword static maps to Private.)
   */
  Private,
  /**
   * Denotes a static member of a class.
   * All members of a module are static by default,
   * the C keyword static maps to Private.
   */
  Static,
  /**
   * null value for pointers.
   */
  Null,
  /**
   * Limit argument to being a pure method.
   */
  Pure,
  /**
   * any value specification value
   */
  Any,
  /**
   * Java final keyword.
   */
  Final,
  /**
   * Full Write Permission
   */
  FullPerm,
  /**
   * Full Write Permission
   */
  ReadPerm,
  /**
   * Full Write Permission
   */
  NoPerm
}
