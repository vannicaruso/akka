/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf

import akka.japi.Option

/**
 * Helper for implementing a *pure* partial function: it will possibly be
 * invoked multiple times for a single “application”, because its only abstract
 * method is used for both isDefinedAt() and apply(); the former is mapped to
 * `onlyCheck == true` and the latter to `onlyCheck == false` for those cases where
 * this is important to know.
 *
 * Failure to match is signaled by returning `None`, and success by returning `Some`.
 */
private[pf] abstract class AbstractPartialFunction[A, B] extends scala.runtime.AbstractPartialFunction[A, B] {

  def apply(x: A, onlyCheck: Boolean): Option[B]

  final def isDefinedAt(x: A): Boolean = apply(x, true).isDefined
  final override def apply(x: A): B = apply(x, false).getOrElse(throw new MatchError(x))
  final override def applyOrElse[A1 <: A, B1 >: B](x: A1, default: A1 ⇒ B1): B1 = apply(x, false).getOrElse(default(x))
}
