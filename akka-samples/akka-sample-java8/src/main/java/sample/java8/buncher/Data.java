package sample.java8.buncher;

import akka.actor.ActorRef;

import java.util.LinkedList;
import java.util.List;

interface Data {

  final class Uninitialized implements Data {

    private static final Uninitialized instance = new Uninitialized();

    public static Uninitialized getInstance() {
      return instance;
    }

    private Uninitialized() {
    }
  }

  final class Todo implements Data {
    final ActorRef target;
    final List<Object> queue;

    public Todo(ActorRef target, List<Object> queue) {
      this.target = target;
      this.queue = queue;
    }

    public ActorRef getTarget() {
      return target;
    }

    public List<Object> getQueue() {
      return queue;
    }

    @Override
    public String toString() {
      return "Todo{" +
        "target=" + target +
        ", queue=" + queue +
        '}';
    }

    public Todo addElement(Object element) {

      List<Object> nQueue = new LinkedList<>(queue);
      nQueue.add(element);
      return new Todo(this.target, nQueue);
    }

    public Todo copy(List<Object> queue) {
      return new Todo(this.target, queue);
    }

    public Todo copy(ActorRef target) {
      return new Todo(target, this.queue);
    }

  }
}
