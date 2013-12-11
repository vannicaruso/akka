package sample.java8;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class Java8Actor extends AbstractActor {

  private PartialFunction<Object, BoxedUnit> guarded = ReceiveBuilder.
    match(String.class, s -> s.contains("guard"), s -> {
      sender().tell("contains(guard): " + s, self());
      context().unbecome();
    }).build();

  @Override
  public PartialFunction<Object, BoxedUnit> receive() {
    return ReceiveBuilder.
      match(Double.class, d -> {
        sender().tell(d, self());
      }).
      match(Integer.class, i -> {
        sender().tell(i, self());
      }).
      match(String.class, s -> s.startsWith("guard"), s -> {
        sender().tell("startsWith(guard): " + s, self());
        context().become(guarded, false);
      }).build();
  }
}
