package sample.java8.buncher;

import akka.actor.ActorRef;

import java.util.List;

public class Messages {

  public static final class SetTarget {
    public final ActorRef ref;

    public SetTarget(ActorRef ref) {
      this.ref = ref;
    }

    public ActorRef getRef() {
      return ref;
    }

    @Override
    public String toString() {
      return "SetTarget{" +
        "ref=" + ref +
        '}';
    }
  }

  public static final class Queue {
    Object obj;

    public Queue(Object obj) {
      this.obj = obj;
    }

    public Object getObj() {
      return obj;
    }

    @Override
    public String toString() {
      return "Queue{" +
        "obj=" + obj +
        '}';
    }
  }

  public static final class Batch {
    private final List<Object> list;

    public Batch(List<Object> list) {
      this.list = list;
    }

    public List<Object> getList() {
      return list;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Batch batch = (Batch) o;

      return list.equals(batch.list);
    }

    @Override
    public int hashCode() {
      return list.hashCode();
    }

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append( "Batch{list=");
      list.stream().forEachOrdered(e -> { builder.append(e); builder.append(","); });
      int len = builder.length();
      builder.replace(len, len, "}");
      return builder.toString();
    }
  }

  public static final class Flush {
    private static final Flush instance = new Flush();

    public static Flush getInstance() {
      return instance;
    }

    Flush() {
    }
  }
}
