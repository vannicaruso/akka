/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import scala.Tuple2;

// TODO:ban #3770 doc missing

public class FSMTransitionHandlerBuilder<S> {
  private UnitMatchBuilder<Tuple2<S, S>> builder = UnitMatch.builder();

  public FSMTransitionHandlerBuilder<S> state(final S fromState,
                                  final S toState,
                                  final SAM.EmptyUnitApply apply) {
    builder.match(Tuple2.class,
      new SAM.TypedPredicate<Tuple2>() {
        @Override
        public boolean defined(Tuple2 t) {
          return fromState.equals(t._1()) && toState.equals(t._2());
        }
      },
      new SAM.UnitApply<Tuple2>() {
        @Override
        public void apply(Tuple2 t) {
          apply.apply();
        }
      }
    );
    return this;
  }

  public PartialFunction<Tuple2<S, S>, BoxedUnit> build() {
    return builder.build();
  }
}
