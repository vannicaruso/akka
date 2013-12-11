/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import akka.japi.Option;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.util.LinkedList;

/**
 * Version of {@link scala.PartialFunction} that can be built during
 * runtime from Java.
 * This is a specialized version of {@link UnitMatch} to map java
 * void methods to {@link scala.runtime.BoxedUnit}.
 *
 * @param <I> the from type, that this PartialFunction will be applied to
 */
public class UnitMatch<I> extends AbstractPartialFunction<I, BoxedUnit> {

  /**
   * Create a {@link UnitMatchBuilder}.
   *
   * @return a builder for a partial function
   */
  public static final <F> UnitMatchBuilder<F> builder() {
    return new UnitMatchBuilder<F>();
  }

  /**
   * Convenience function to create a {@link UnitMatchBuilder} with the first
   * case statement added.
   *
   * @param type   a type to match the argument against
   * @param apply  an action to apply to the argument if the type matches
   * @return       a builder with the case statement added
   * @see UnitMatchBuilder#match(Class, SAM.UnitApply)
   */
  public static final <F, P> UnitMatchBuilder<F> match(final Class<P> type, SAM.UnitApply<P> apply) {
    return new UnitMatchBuilder<F>().match(type, apply);
  }

  /**
   * Convenience function to create a {@link UnitMatchBuilder} with the first
   * case statement added.
   *
   * @param type       a type to match the argument against
   * @param predicate  a predicate that will be evaluated on the argument if the type matches
   * @param apply      an action to apply to the argument if the type matches
   * @return           a builder with the case statement added
   * @see UnitMatchBuilder#match(Class, SAM.TypedPredicate, SAM.UnitApply)
   */
  public static <F, P> UnitMatchBuilder<F> match(final Class<P> type,
                                                 final SAM.TypedPredicate<P> predicate,
                                                 SAM.UnitApply<P> apply) {
    return new UnitMatchBuilder<F>().match(type, predicate, apply);
  }

  /**
   * Create a {@link UnitMatch} from the builder.
   *
   * @param builder  a builder representing the partial function
   * @return         a {@link UnitMatch} that can be reused
   */
  public static <F> UnitMatch<F> create(UnitMatchBuilder<F> builder) {
    return (UnitMatch<F>) builder.build();
  }

  private final Option<BoxedUnit> some = new Option.Some<BoxedUnit>(null);
  private final Option<BoxedUnit> none = Option.none();

  private final LinkedList<UnitCaseStatement> statements;

  UnitMatch(LinkedList<UnitCaseStatement> statements) {
    this.statements = statements;
  }

  @Override
  public Option<BoxedUnit> apply(Object o, boolean onlyCheck) {
    for (UnitCaseStatement s: statements) {
      if (s.defined(o)) {
        if (!onlyCheck) {
          s.apply(o);
        }
        return some;
      }
    }
    return none;
  }

  /**
   * Convenience function to make the Java code more readable.
   *
   * <pre><code>
   *   UnitMatcher&lt;X&gt; matcher = UnitMatcher.create(...);
   *
   *   matcher.match(obj);
   * </code></pre>
   *
   * @param i  the argument to apply the match to
   */
  public void match(I i) {
    apply(i);
  }

  /**
   * Turn this {@link UnitMatch} into a {@link scala.PartialFunction}.
   *
   * @return  a partial function representation ot his {@link UnitMatch}
   */
  public PartialFunction<I, BoxedUnit> asPF() {
    return (PartialFunction<I, BoxedUnit>) this;
  }
}
