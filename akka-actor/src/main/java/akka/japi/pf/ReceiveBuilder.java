/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

/**
 * Used for building a partial function for {@link akka.actor.Actor#receive() Actor.receive()}.
 *
 * There are both a match on type only, and a match on type and predicate.
 *
 * Inside an actor you can use it like this with Java 8 to define your receive method:
 * <pre>
 * @Override
 * public PartialFunction<Object, BoxedUnit> receive() {
 *   return ReceiveBuilder.
 *     match(Double.class, d -> {
 *       sender().tell(d, self());
 *     }).
 *     match(Integer.class, i -> {
 *       sender().tell(i, self());
 *     }).
 *     match(String.class, s -> s.startsWith("foo"), s -> {
 *       sender().tell(s, self());
 *     }).build();
 * }
 * </pre>
 *
 */
public class ReceiveBuilder {
  private ReceiveBuilder() {
  }

  // TODO:ban #3770 doc missing
  public static <P> UnitMatchBuilder<Object> match(final Class<P> type, SAM.UnitApply<P> apply) {
    return UnitMatch.match(type, apply);
  }

  // TODO:ban #3770 doc missing
  public static <P> UnitMatchBuilder<Object> match(final Class<P> type,
                                                   SAM.TypedPredicate<P> predicate,
                                                   SAM.UnitApply<P> apply) {
    return UnitMatch.match(type, predicate, apply);
  }
}
