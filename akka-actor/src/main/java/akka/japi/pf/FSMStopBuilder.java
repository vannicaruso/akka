/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import akka.actor.FSM;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

// TODO:ban #3770 doc missing

public class FSMStopBuilder<S, D> {

  private UnitMatchBuilder<FSM<S, D>.StopEvent> builder = UnitMatch.builder();

  public FSMStopBuilder<S, D> stop(final FSM.Reason reason,
                                   final SAM.UnitApply2<S, D> apply) {
    builder.match(FSM.StopEvent.class,
      new SAM.TypedPredicate<FSM.StopEvent>() {
        @Override
        public boolean defined(FSM.StopEvent e) {
          return reason.equals(e.reason());
        }
      },
      new SAM.UnitApply<FSM.StopEvent>() {
        public void apply(FSM.StopEvent e) {
          @SuppressWarnings("unchecked")
          S s = (S) e.currentState();
          @SuppressWarnings("unchecked")
          D d = (D) e.stateData();
          apply.apply(s, d);
        }
      }
    );

    return this;
  }

  public <P extends FSM.Reason> FSMStopBuilder<S, D> stop(final Class<P> reasonType,
                                                          final SAM.UnitApply3<P, S, D> apply) {
    return this.stop(reasonType,
      new SAM.TypedPredicate<P>() {
        @Override
        public boolean defined(P p) {
          return true;
        }
      }, apply);
  }

  public <P extends FSM.Reason> FSMStopBuilder<S, D> stop(final Class<P> reasonType,
                                                          final SAM.TypedPredicate<P> predicate,
                                                          final SAM.UnitApply3<P, S, D> apply) {
    builder.match(FSM.StopEvent.class,
      new SAM.TypedPredicate<FSM.StopEvent>() {
        @Override
        public boolean defined(FSM.StopEvent e) {
          if (reasonType.isInstance(e.reason())) {
            @SuppressWarnings("unchecked")
            P p = (P) e.reason();
            return predicate.defined(p);
          } else {
            return false;
          }
        }
      },
      new SAM.UnitApply<FSM.StopEvent>() {
        public void apply(FSM.StopEvent e) {
          @SuppressWarnings("unchecked")
          P p = (P) e.reason();
          @SuppressWarnings("unchecked")
          S s = (S) e.currentState();
          @SuppressWarnings("unchecked")
          D d = (D) e.stateData();
          apply.apply(p, s, d);
        }
      }
    );

    return this;
  }

  public PartialFunction<FSM<S, D>.StopEvent, BoxedUnit> build() {
    return builder.build();
  }
}
