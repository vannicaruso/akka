/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import java.util.LinkedList;
import scala.PartialFunction;

/**
 * A builder for {@link scala.PartialFunction}.
 *
 * @param <F> the from type, that this PartialFunction will be applied to
 * @param <T> the to type, that the results of the application will have
 */
public final class MatchBuilder<F, T> {

  /**
   * Package scoped constructor.
   * Create builders using {@link Match#builder Match.builder()}.
   */
  MatchBuilder() {
  }

  private LinkedList<CaseStatement> statements = new LinkedList<CaseStatement>();

  /**
   * Add a new case statement to this builder.
   *
   * @param type   a type to match the argument against
   * @param apply  an action to apply to the argument if the type matches
   * @return       a builder with the case statement added
   */
  public <P> MatchBuilder<F, T> match(final Class<P> type, SAM.Apply<P, T> apply) {
    statements.add(new CaseStatement<P, T>(
      new SAM.Predicate<P>() {
        @Override
        public boolean defined(Object o) {
          return type.isInstance(o);
        }
      }, apply));
    return this;
  }

  /**
   * Add a new case statement to this builder.
   *
   * @param type       a type to match the argument against
   * @param predicate  a predicate that will be evaluated on the argument if the type matches
   * @param apply      an action to apply to the argument if the type matches and the predicate returns true
   * @return           a builder with the case statement added
   */
  public <P> MatchBuilder<F, T> match(final Class<P> type,
                                      final SAM.TypedPredicate<P> predicate,
                                      SAM.Apply<P, T> apply) {
    statements.add(new CaseStatement<P, T>(
      new SAM.Predicate<P>() {
        @Override
        public boolean defined(Object o) {
          if (!type.isInstance(o))
            return false;
          else {
            @SuppressWarnings("unchecked")
            P p = (P) o;
            return predicate.defined(p);
          }
        }
      }, apply));
    return this;
  }

  /**
   * Build a {@link scala.PartialFunction} from this builder.
   * After this call the builder will be reset.
   *
   * @return  a PartialFunction for this builder.
   */
  public PartialFunction<F, T> build() {
    Match<F, T> match = new Match<F, T>(statements);
    statements = null;
    return (PartialFunction<F, T>) match;
  }
}
