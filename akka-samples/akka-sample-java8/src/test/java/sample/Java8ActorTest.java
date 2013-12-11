package sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import sample.java8.Java8Actor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class Java8ActorTest {

  static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create("8ActorTest");
  }

  @AfterClass
  public static void tearDown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testJ8Actor()
  {
    new JavaTestKit(system) {{
      final ActorRef j8Ref = system.actorOf(Props.create(Java8Actor.class), "java8-actor");
      final ActorRef probeRef = getRef();

      j8Ref.tell(47.11, probeRef);
      j8Ref.tell("and no guard in the beginning", probeRef);
      j8Ref.tell("guard is a good thing", probeRef);
      j8Ref.tell(47.11, probeRef);
      j8Ref.tell(4711, probeRef);
      j8Ref.tell("and no guard in the beginning", probeRef);
      j8Ref.tell(4711, probeRef);
      j8Ref.tell("and an unmatched message", probeRef);

      expectMsgEquals(47.11);
      assertTrue(expectMsgClass(String.class).startsWith("startsWith(guard):"));
      assertTrue(expectMsgClass(String.class).startsWith("contains(guard):"));
      expectMsgEquals(4711);
      expectNoMsg();
    }};
  }
}
