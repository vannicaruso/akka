/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

/**
 * Representation of a case statement in a {@link MatchBuilder}.
 *
 * @param <P>  the type for the argument that the
 *             {@link SAM.Predicate Predicate} and the
 *             {@link SAM.Apply Apply} expects
 * @param <R>  the return type of the {@link SAM.Apply Apply}
 */
final class CaseStatement<P, R> {
  private final SAM.Predicate<P> predicate;
  private final SAM.Apply<P, R> apply;

  CaseStatement(SAM.Predicate<P> predicate, SAM.Apply<P, R> apply) {
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
   * @return   the result of the function
   */
  R apply(Object o) {
    @SuppressWarnings("unchecked")
    P p = (P) o;
    return apply.apply(p);
  }
}
