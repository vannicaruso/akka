package sample.java8.buncher;

import akka.actor.AbstractFSM;
import akka.japi.pf.UnitMatch;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.LinkedList;

import static sample.java8.buncher.Data.*;
import static sample.java8.buncher.State.*;
import static sample.java8.buncher.Messages.*;

public class Buncher extends AbstractFSM<State, Data> {

  {
    startWith(Idle, Uninitialized.getInstance());

    when(Idle,
      matchEvent(SetTarget.class, Uninitialized.class,
        (st, u) -> stay().using(new Todo(st.ref, new LinkedList<>()))));

    onTransition(
      matchState(Active, Idle, () -> {
        // reuse this matcher
        final UnitMatch<Data> m = UnitMatch.create(
          matchData(Todo.class,
            t -> t.getTarget().tell(new Batch(t.getQueue()), self())));
        m.match(stateData());
      }).
      state(Idle, Active, () -> {/* Do something here */}));

    when(Active, Duration.create(1, "second"),
      matchEvent(Arrays.asList(Flush.class, StateTimeout()), Todo.class,
        t -> goTo(Idle).using(t.copy(new LinkedList<>()))).build());

    whenUnhandled(
      matchEvent(Queue.class, Todo.class,
        (q, t) -> goTo(Active).using(t.addElement(q.getObj()))).
      anyEvent((e, s) -> {
        log().warning("received unhandled request {} in state {}/{}", e, stateName(), s);
        return stay();
      }));

    onTermination(
      matchStop(Normal(),
        (state, data) -> {/* Do something here */}).
      stop(Shutdown(),
        (state, data) -> {/* Do something here */}).
      stop(Failure(),
        (reason, state, data) -> {/* Do something here */}));

    initialize();
  }
}
