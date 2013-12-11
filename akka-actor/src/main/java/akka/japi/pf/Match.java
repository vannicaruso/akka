/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import akka.japi.Option;
import java.util.LinkedList;
import scala.PartialFunction;

/**
 * Version of {@link scala.PartialFunction} that can be built during
 * runtime from Java.
 *
 * @param <I> the from type, that this PartialFunction will be applied to
 * @param <O> the to type, that the results of the application will have
 */
public class Match<I, O> extends AbstractPartialFunction<I, O> {

  /**
   * Create a {@link MatchBuilder}.
   *
   * @return a builder for a partial function
   */
  public static final <F, T> MatchBuilder<F, T> builder() {
    return new MatchBuilder<F, T>();
  }

  /**
   * Convenience function to create a {@link MatchBuilder} with the first
   * case statement added.
   *
   * @param type   a type to match the argument against
   * @param apply  an action to apply to the argument if the type matches
   * @return       a builder with the case statement added
   * @see MatchBuilder#match(Class, SAM.Apply)
   */
  public static final <F, T, P> MatchBuilder<F, T> match(final Class<P> type,
                                                         SAM.Apply<P, T> apply) {
    return new MatchBuilder<F, T>().match(type, apply);
  }

  /**
   * Convenience function to create a {@link MatchBuilder} with the first
   * case statement added.
   *
   * @param type       a type to match the argument against
   * @param predicate  a predicate that will be evaluated on the argument if the type matches
   * @param apply      an action to apply to the argument if the type matches
   * @return           a builder with the case statement added
   * @see MatchBuilder#match(Class, SAM.TypedPredicate, SAM.Apply)
   */
  public static <F, T, P> MatchBuilder<F, T> match(final Class<P> type,
                                                   final SAM.TypedPredicate<P> predicate,
                                                   SAM.Apply<P, T> apply) {
    return new MatchBuilder<F, T>().match(type, predicate, apply);
  }

  /**
   * Create a {@link Match} from the builder.
   *
   * @param builder  a builder representing the partial function
   * @return         a {@link Match} that can be reused
   */
  public static final <F, T> Match<F, T> create(MatchBuilder<F, T> builder) {
    return (Match<F, T>) builder.build();
  }

  private final Option<O> some = new Option.Some<O>(null);
  private final Option<O> none = Option.none();

  private final LinkedList<CaseStatement> statements;

  Match(LinkedList<CaseStatement> statements) {
    this.statements = statements;
  }

  @Override
  public Option<O> apply(Object o, boolean onlyCheck) {
    for (CaseStatement s: statements) {
      if (s.defined(o)) {
        if (!onlyCheck) {
          @SuppressWarnings("unchecked")
          Option.Some<O> ret = new Option.Some<O>((O) s.apply(o));
          return ret;
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
   *   Matcher&lt;X, Y&gt; matcher = Matcher.create(...);
   *
   *   Y someY = matcher.match(obj);
   * </code></pre>
   *
   * @param i  the argument to apply the match to
   * @return   the result of the application
   */
  public O match(I i) {
    return apply(i);
  }

  /**
   * Turn this {@link Match} into a {@link scala.PartialFunction}.
   *
   * @return  a partial function representation ot his {@link Match}
   */
  public PartialFunction<I, O> asPF() {
    return (PartialFunction<I, O>) this;
  }

}
