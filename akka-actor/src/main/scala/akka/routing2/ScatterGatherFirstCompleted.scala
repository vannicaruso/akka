/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.routing2

import scala.collection.immutable
import akka.actor.ActorContext
import akka.actor.Props
import akka.dispatch.Dispatchers
import com.typesafe.config.Config
import akka.actor.SupervisorStrategy
import akka.routing.RouterConfig
import akka.japi.Util.immutableSeq
import akka.actor.ActorRef
import scala.concurrent.Promise
import akka.pattern.ask
import akka.pattern.pipe
import akka.dispatch.ExecutionContexts
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import akka.util.Timeout
import java.util.concurrent.TimeUnit

object ScatterGatherFirstCompletedRoutingLogic {
  def apply(within: FiniteDuration): ScatterGatherFirstCompletedRoutingLogic =
    new ScatterGatherFirstCompletedRoutingLogic(within)
}

class ScatterGatherFirstCompletedRoutingLogic(within: FiniteDuration) extends RoutingLogic {
  override def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee =
    if (routees.isEmpty) NoRoutee
    else ScatterGatherFirstCompletedRoutees(routees, within)
}

/**
 * INTERNAL API
 */
private[akka] case class ScatterGatherFirstCompletedRoutees(
  routees: immutable.IndexedSeq[Routee], within: FiniteDuration) extends Routee {

  override def send(message: Any, sender: ActorRef): Unit = {
    // FIXME #3549 old impl used PromiseActorRef, why?
    implicit val ec = ExecutionContexts.sameThreadExecutionContext
    implicit val timeout = Timeout(within)
    val promise = Promise[Any]()
    routees.foreach {
      case ActorRefRoutee(ref) ⇒
        promise.tryCompleteWith(ref.ask(message))
      case ActorSelectionRoutee(sel) ⇒
        promise.tryCompleteWith(sel.ask(message))
      case _ ⇒
    }

    promise.future.pipeTo(sender)
  }
}

final case class ScatterGatherFirstCompletedPool(
  override val nrOfInstances: Int, override val resizer2: Option[Resizer] = None,
  within: FiniteDuration,
  override val supervisorStrategy: SupervisorStrategy = RouterConfig2.defaultSupervisorStrategy,
  override val routerDispatcher: String = Dispatchers.DefaultDispatcherId)
  extends Pool with PoolOverrideUnsetConfig[ScatterGatherFirstCompletedPool] {

  def this(config: Config) =
    this(
      nrOfInstances = config.getInt("nr-of-instances"),
      within = Duration(config.getMilliseconds("within"), TimeUnit.MILLISECONDS),
      resizer2 = DefaultResizer.fromConfig(config))

  override def createRouter(): Router = new Router(ScatterGatherFirstCompletedRoutingLogic(within))

  /**
   * Setting the supervisor strategy to be used for the “head” Router actor.
   */
  def withSupervisorStrategy(strategy: SupervisorStrategy): ScatterGatherFirstCompletedPool = copy(supervisorStrategy = strategy)

  /**
   * Setting the resizer to be used.
   */
  def withResizer(resizer: Resizer): ScatterGatherFirstCompletedPool = copy(resizer2 = Some(resizer))
}

final case class ScatterGatherFirstCompletedNozzle(
  paths: immutable.Iterable[String],
  within: FiniteDuration,
  override val supervisorStrategy: SupervisorStrategy = RouterConfig2.defaultSupervisorStrategy,
  override val routerDispatcher: String = Dispatchers.DefaultDispatcherId)
  extends Nozzle with NozzleOverrideUnsetConfig[ScatterGatherFirstCompletedNozzle] {

  def this(config: Config) =
    this(
      paths = immutableSeq(config.getStringList("routees.paths")),
      within = Duration(config.getMilliseconds("within"), TimeUnit.MILLISECONDS))

  override def createRouter(): Router = new Router(ScatterGatherFirstCompletedRoutingLogic(within))

  /**
   * Setting the supervisor strategy to be used for the “head” Router actor.
   */
  override def withSupervisorStrategy(strategy: SupervisorStrategy): ScatterGatherFirstCompletedNozzle = copy(supervisorStrategy = strategy)

  /**
   * Uses the resizer and/or the supervisor strategy of the given Routerconfig
   * if this RouterConfig doesn't have one, i.e. the resizer defined in code is used if
   * resizer was not defined in config.
   */
  override def withFallback(other: RouterConfig): RouterConfig = this.overrideUnsetConfig(other)

}
