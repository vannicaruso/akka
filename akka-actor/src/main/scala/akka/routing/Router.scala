/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.routing

import scala.collection.immutable
import akka.actor.ActorRef
import akka.actor.ActorSelection

trait RoutingLogic {
  def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee
}

trait Routee {
  def send(message: Any, sender: ActorRef): Unit
}

case class ActorRefRoutee(ref: ActorRef) extends Routee {
  override def send(message: Any, sender: ActorRef): Unit =
    ref.tell(message, sender)
}

case class ActorSelectionRoutee(selection: ActorSelection) extends Routee {
  override def send(message: Any, sender: ActorRef): Unit =
    selection.tell(message, sender)
}

object NoRoutee extends Routee {
  // FIXME #3549 not deadLetters any more?
  override def send(message: Any, sender: ActorRef): Unit = ()
}

case class SeveralRoutees(routees: immutable.IndexedSeq[Routee]) extends Routee {
  override def send(message: Any, sender: ActorRef): Unit =
    routees.foreach(_.send(message, sender))
}

final case class Router(val logic: RoutingLogic, val routees: immutable.IndexedSeq[Routee] = Vector.empty) {

  // FIXME #3549 Java api

  /**
   * If the message is a [[akka.routing.RouterEnvelope]] it will be
   * unwrapped before sent to the destinations.
   */
  def route(message: Any, sender: ActorRef): Unit =
    message match {
      case akka.routing.Broadcast(msg) ⇒ SeveralRoutees(routees).send(msg, sender)
      case msg                         ⇒ logic.select(msg, routees).send(unwrap(msg), sender)
    }

  private def unwrap(msg: Any): Any = msg match {
    case env: RouterEnvelope ⇒ env.message
    case _                   ⇒ msg
  }

  def withRoutees(rs: immutable.IndexedSeq[Routee]): Router = copy(routees = rs)

  def addRoutee(routee: Routee): Router = copy(routees = routees :+ routee)

  def addRoutee(ref: ActorRef): Router = addRoutee(ActorRefRoutee(ref))

  def addRoutee(sel: ActorSelection): Router = addRoutee(ActorSelectionRoutee(sel))

  def removeRoutee(routee: Routee): Router = copy(routees = routees.filterNot(_ == routee))

  def removeRoutee(ref: ActorRef): Router = removeRoutee(ActorRefRoutee(ref))

  def removeRoutee(sel: ActorSelection): Router = removeRoutee(ActorSelectionRoutee(sel))

}

/**
 * Used to broadcast a message to all connections in a router; only the
 * contained message will be forwarded, i.e. the `Broadcast(...)`
 * envelope will be stripped off.
 *
 * Router implementations may choose to handle this message differently.
 */
@SerialVersionUID(1L)
case class Broadcast(message: Any) extends RouterEnvelope

/**
 * Only the contained message will be forwarded to the
 * destination, i.e. the envelope will be stripped off.
 */
trait RouterEnvelope {
  def message: Any
}

