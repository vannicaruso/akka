/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import java.util.LinkedList;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

/**
 * A builder for {@link scala.PartialFunction}.
 * This is a specialized version of {@link MatchBuilder} to map java
 * void methods to {@link scala.runtime.BoxedUnit}.
 *
 * @param <F> the from type, that this PartialFunction to be applied to
 */
public final class UnitMatchBuilder<F> {

  /**
   * Package scoped constructor.
   * Create builders using {@link UnitMatch#builder UnitMatch.builder()}.
   */
  UnitMatchBuilder() {
  }

  private LinkedList<UnitCaseStatement> statements = new LinkedList<UnitCaseStatement>();

  /**
   * Add a new case statement to this builder.
   *
   * @param type   a type to match the argument against
   * @param apply  an action to apply to the argument if the type matches
   * @return       a builder with the case statement added
   */
  public <P> UnitMatchBuilder<F> match(final Class<P> type, SAM.UnitApply<P> apply) {
    statements.add(new UnitCaseStatement<P>(
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
  public <P> UnitMatchBuilder<F> match(final Class<P> type,
                                       final SAM.TypedPredicate<P> predicate,
                                       SAM.UnitApply<P> apply) {
    statements.add(new UnitCaseStatement<P>(
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
  public PartialFunction<F, BoxedUnit> build() {
    UnitMatch<F> match = new UnitMatch<F>(statements);
    statements = null;
    return match.asPF();
  }
}
