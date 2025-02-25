package vct.col.ast.expr.literal.build

import vct.col.ast.{OptSomeTyped, TOption, Type}

trait OptSomeTypedImpl[G] { this: OptSomeTyped[G] =>
  override def t: Type[G] = TOption(element)
}