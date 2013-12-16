/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.japi.pf;

import akka.actor.FSM;
import scala.PartialFunction;
import java.util.List;

// TODO:ban #3770 doc missing

public class FSMStateFunctionBuilder<S, D> {

  private MatchBuilder<FSM.Event<D>, FSM.State<S, D>> builder = Match.builder();

  public <P, Q> FSMStateFunctionBuilder<S, D> event(final Class<P> eventType,
                                            final Class<Q> dataType,
                                            final SAM.Apply2<P, Q, FSM.State<S, D>> apply) {
    builder.match(FSM.Event.class,
      new SAM.TypedPredicate<FSM.Event>() {
        @Override
        public boolean defined(FSM.Event e) {
          return eventType.isInstance(e.event()) && dataType.isInstance(e.stateData());
        }
      },
      new SAM.Apply<FSM.Event, FSM.State<S, D>>() {
        public FSM.State<S, D> apply(FSM.Event e) {
          @SuppressWarnings("unchecked")
          P p = (P) e.event();
          @SuppressWarnings("unchecked")
          Q q = (Q) e.stateData();
          return apply.apply(p, q);
        }
      }
    );

    return this;
  }


  public <Q> FSMStateFunctionBuilder<S, D> event(final List<Object> eventMatches,
                                         final Class<Q> dataType,
                                         final SAM.Apply<Q, FSM.State<S, D>> apply) {
    builder.match(FSM.Event.class,
      new SAM.TypedPredicate<FSM.Event>() {
        @Override
        public boolean defined(FSM.Event e) {
          if (!dataType.isInstance(e.stateData()))
            return false;

          boolean emMatch = false;
          Object event = e.event();
          for (Object em : eventMatches) {
            if (em instanceof Class) {
              Class emc = (Class) em;
              emMatch = emc.isInstance(event);
            } else {
              emMatch = event.equals(em);
            }
            if (emMatch)
              break;
          }
          return emMatch;
        }
      },
      new SAM.Apply<FSM.Event, FSM.State<S, D>>() {
        public FSM.State<S, D> apply(FSM.Event e) {
          @SuppressWarnings("unchecked")
          Q q = (Q) e.stateData();
          return apply.apply(q);
        }
      }
    );

    return this;
  }

  public FSMStateFunctionBuilder<S, D> anyEvent(final SAM.Apply2<Object, D, FSM.State<S, D>> apply) {
    builder.match(FSM.Event.class,
      new SAM.Apply<FSM.Event, FSM.State<S, D>>() {
        public FSM.State<S, D> apply(FSM.Event e) {
          @SuppressWarnings("unchecked")
          D d = (D) e.stateData();
          return apply.apply(e.event(), d);
        }
      });

    return this;
  }

  public PartialFunction<FSM.Event<D>, FSM.State<S, D>> build() {
    return builder.build();
  }
}
