/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

/**
 * Representation of a case statement in a {@link UnitMatchBuilder}.
 * This is a specialized version of {@link CaseStatement} to map java
 * void methods to {@link scala.runtime.BoxedUnit}.
 *
 * @param <P>  the type for the argument that the
 *             {@link SAM.Predicate Predicate} and the
 *             {@link SAM.UnitApply UnitApply} expects
 */
final class UnitCaseStatement<P> {
  private final SAM.Predicate<P> predicate;
  private final SAM.UnitApply<P> apply;

  UnitCaseStatement(SAM.Predicate<P> predicate, SAM.UnitApply<P> apply) {
    this.predicate = predicate;
    this.apply = apply;
  }

  /**
   * Check if the predicate matches this argument.
   *
   * @param o  the argument to check
   * @return   true if the predicate matches, false otherwise
   */
  boolean defined(Object o) {
    return predicate.defined(o);
  }

  /**
   * Apply the function to the argument.
   *
   * @param o  the argument to apply the function to
   */
  void apply(Object o) {
    @SuppressWarnings("unchecked")
    P p = (P) o;
    apply.apply(p);
  }
}
