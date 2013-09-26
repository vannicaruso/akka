/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.cluster.routing

import scala.collection.immutable
import akka.routing.RouterConfig
import akka.routing.Router
import akka.actor.Props
import akka.actor.ActorContext
import akka.routing.Routee
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.Address
import akka.actor.ActorCell
import akka.actor.Deploy
import com.typesafe.config.ConfigFactory
import akka.routing.ActorRefRoutee
import akka.remote.RemoteScope
import akka.actor.Actor
import akka.actor.SupervisorStrategy
import akka.routing.Resizer
import akka.routing.RouterConfig
import akka.routing.Pool
import akka.routing.Nozzle
import akka.remote.routing.RemoteRouterConfig
import akka.routing.RouterActor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorRef
import akka.cluster.Member
import scala.annotation.tailrec
import akka.actor.RootActorPath
import akka.cluster.MemberStatus
import akka.routing.ActorSelectionRoutee
import akka.actor.ActorInitializationException
import akka.routing.RouterPoolActor
import akka.actor.ActorSystem
import akka.actor.ActorSystem
import akka.routing.RoutingLogic
import akka.actor.RelativeActorPath
import com.typesafe.config.Config
import akka.routing.DeprecatedRouterConfig

object ClusterRouterSettings {
  /**
   * Settings for create and deploy of the routees
   */
  def apply(totalInstances: Int, maxInstancesPerNode: Int, allowLocalRoutees: Boolean, useRole: Option[String]): ClusterRouterSettings =
    new ClusterRouterSettings(totalInstances, maxInstancesPerNode, routeesPath = "", allowLocalRoutees, useRole)

  /**
   * Settings for remote deployment of the routees, allowed to use routees on own node
   */
  def apply(totalInstances: Int, maxInstancesPerNode: Int, useRole: Option[String]): ClusterRouterSettings =
    apply(totalInstances, maxInstancesPerNode, allowLocalRoutees = true, useRole)

  /**
   * Settings for lookup of the routees
   */
  def apply(totalInstances: Int, routeesPath: String, allowLocalRoutees: Boolean, useRole: Option[String]): ClusterRouterSettings =
    new ClusterRouterSettings(totalInstances, maxInstancesPerNode = 1, routeesPath, allowLocalRoutees, useRole)

  /**
   * Settings for lookup of the routees, allowed to use routees on own node
   */
  def apply(totalInstances: Int, routeesPath: String, useRole: Option[String]): ClusterRouterSettings =
    apply(totalInstances, routeesPath, allowLocalRoutees = true, useRole)

  def useRoleOption(role: String): Option[String] = role match {
    case null | "" ⇒ None
    case _         ⇒ Some(role)
  }

  /**
   * Create settings from deployment config
   */
  def fromConfig(config: Config): ClusterRouterSettings =
    ClusterRouterSettings(
      totalInstances = config.getInt("nr-of-instances"),
      maxInstancesPerNode = config.getInt("cluster.max-nr-of-instances-per-node"),
      allowLocalRoutees = config.getBoolean("cluster.allow-local-routees"),
      routeesPath = config.getString("cluster.routees-path"),
      useRole = useRoleOption(config.getString("cluster.use-role")))
}

/**
 * `totalInstances` of cluster router must be > 0
 * `maxInstancesPerNode` of cluster router must be > 0
 * `maxInstancesPerNode` of cluster router must be 1 when routeesPath is defined
 */
@SerialVersionUID(1L)
case class ClusterRouterSettings private[akka] (
  totalInstances: Int,
  maxInstancesPerNode: Int,
  routeesPath: String,
  allowLocalRoutees: Boolean,
  useRole: Option[String]) {

  /**
   * Java API: Settings for create and deploy of the routees
   */
  def this(totalInstances: Int, maxInstancesPerNode: Int, allowLocalRoutees: Boolean, useRole: String) =
    this(totalInstances, maxInstancesPerNode, routeesPath = "", allowLocalRoutees,
      ClusterRouterSettings.useRoleOption(useRole))

  /**
   * Java API: Settings for lookup of the routees
   */
  def this(totalInstances: Int, routeesPath: String, allowLocalRoutees: Boolean, useRole: String) =
    this(totalInstances, maxInstancesPerNode = 1, routeesPath, allowLocalRoutees,
      ClusterRouterSettings.useRoleOption(useRole))

  if (totalInstances <= 0) throw new IllegalArgumentException("totalInstances of cluster router must be > 0")
  if (maxInstancesPerNode <= 0) throw new IllegalArgumentException("maxInstancesPerNode of cluster router must be > 0")
  if (isRouteesPathDefined && maxInstancesPerNode != 1)
    throw new IllegalArgumentException("maxInstancesPerNode of cluster router must be 1 when routeesPath is defined")

  routeesPath match {
    case RelativeActorPath(elements) ⇒ // good
    case _ ⇒
      throw new IllegalArgumentException("routeesPath [%s] is not a valid relative actor path" format routeesPath)
  }

  def isRouteesPathDefined: Boolean = (routeesPath ne null) && routeesPath != ""

}

/**
 * [[akka.routing.RouterConfig]] implementation for deployment on cluster nodes.
 * Delegates other duties to the local [[akka.routing.RouterConfig]],
 * which makes it possible to mix this with the built-in routers such as
 * [[akka.routing.RoundRobinRouter]] or custom routers.
 */
@SerialVersionUID(1L)
final case class ClusterNozzle(local: Nozzle, settings: ClusterRouterSettings) extends Nozzle with ClusterRouterConfigBase {

  require(settings.routeesPath.nonEmpty, "routeesPath must be defined")

  override def paths: immutable.Iterable[String] = if (settings.allowLocalRoutees) List(settings.routeesPath) else Nil

  /**
   * INTERNAL API
   */
  override private[akka] def createRouterActor(): RouterActor = new ClusterNozzleActor(settings)

  override def withFallback(other: RouterConfig): RouterConfig = other match {
    case ClusterNozzle(_: ClusterNozzle, _) ⇒ throw new IllegalStateException(
      "ClusterNozzle is not allowed to wrap a ClusterNozzle")
    case ClusterNozzle(local, _) ⇒
      // FIXME #3549 is this always correct?
      copy(local = this.local.withFallback(local).asInstanceOf[Nozzle])
    case ClusterRouterConfig(local, _) ⇒
      // FIXME #3549 is this always correct?
      copy(local = this.local.withFallback(local).asInstanceOf[Nozzle])
    case _ ⇒
      copy(local = this.local.withFallback(other).asInstanceOf[Nozzle])
  }

}

/**
 * [[akka.routing.RouterConfig]] implementation for deployment on cluster nodes.
 * Delegates other duties to the local [[akka.routing.RouterConfig]],
 * which makes it possible to mix this with the built-in routers such as
 * [[akka.routing.RoundRobinRouter]] or custom routers.
 */
@SerialVersionUID(1L)
final case class ClusterPool(local: Pool, settings: ClusterRouterSettings) extends Pool with ClusterRouterConfigBase {

  require(local.resizer.isEmpty, "Resizer can't be used together with cluster router")

  @transient private val childNameCounter = new AtomicInteger

  /**
   * INTERNAL API
   */
  override private[akka] def newRoutee(routeeProps: Props, context: ActorContext): Routee = {
    val name = "c" + childNameCounter.incrementAndGet
    val ref = context.asInstanceOf[ActorCell].attachChild(routeeProps, name, systemService = false)
    ActorRefRoutee(ref)
  }

  /**
   * Initial number of routee instances
   */
  override def nrOfInstances: Int = if (settings.allowLocalRoutees) settings.maxInstancesPerNode else 0

  override def resizer: Option[Resizer] = local.resizer

  /**
   * INTERNAL API
   */
  override private[akka] def createRouterActor(): RouterActor = new ClusterPoolActor(local.supervisorStrategy, settings)

  override def supervisorStrategy: SupervisorStrategy = local.supervisorStrategy

  override def withFallback(other: RouterConfig): RouterConfig = other match {
    case ClusterPool(_: ClusterPool, _) ⇒ throw new IllegalStateException(
      "ClusterPool is not allowed to wrap a ClusterPool")
    case ClusterPool(otherLocal, _) ⇒
      // FIXME #3549 is this always correct?
      copy(local = this.local.withFallback(otherLocal).asInstanceOf[Pool])
    case ClusterRouterConfig(otherLocal, _) ⇒
      // FIXME #3549 is this always correct?
      copy(local = this.local.withFallback(otherLocal).asInstanceOf[Pool])
    case _ ⇒
      copy(local = this.local.withFallback(other).asInstanceOf[Pool])
  }

}

/**
 * INTERNAL API
 */
private[akka] trait ClusterRouterConfigBase extends RouterConfig {
  def local: RouterConfig
  def settings: ClusterRouterSettings
  override def createRouter(system: ActorSystem): Router = local.createRouter(system)
  override def routerDispatcher: String = local.routerDispatcher
  override def stopRouterWhenAllRouteesRemoved: Boolean = false
  override def routingLogicController(routingLogic: RoutingLogic): Option[Props] =
    local.routingLogicController(routingLogic)

  // Intercept ClusterDomainEvent and route them to the ClusterRouterActor
  override def isManagementMessage(msg: Any): Boolean =
    (msg.isInstanceOf[ClusterDomainEvent]) || super.isManagementMessage(msg)
}

@deprecated("Use ClusterPool or ClusterNozzle", "2.3")
@SerialVersionUID(1L)
final case class ClusterRouterConfig(local: DeprecatedRouterConfig, settings: ClusterRouterSettings) extends DeprecatedRouterConfig with ClusterRouterConfigBase {

  require(local.resizer.isEmpty, "Resizer can't be used together with cluster router")

  @transient private val childNameCounter = new AtomicInteger

  /**
   * INTERNAL API
   */
  override private[akka] def newRoutee(routeeProps: Props, context: ActorContext): Routee = {
    val name = "c" + childNameCounter.incrementAndGet
    val ref = context.asInstanceOf[ActorCell].attachChild(routeeProps, name, systemService = false)
    ActorRefRoutee(ref)
  }

  override def nrOfInstances: Int = if (settings.allowLocalRoutees) settings.maxInstancesPerNode else 0

  override def paths: immutable.Iterable[String] =
    if (settings.allowLocalRoutees && settings.routeesPath.nonEmpty) List(settings.routeesPath) else Nil

  override def resizer: Option[Resizer] = local.resizer

  /**
   * INTERNAL API
   */
  override private[akka] def createRouterActor(): RouterActor =
    if (settings.routeesPath.isEmpty)
      new ClusterPoolActor(local.supervisorStrategy, settings)
    else
      new ClusterNozzleActor(settings)

  override def supervisorStrategy: SupervisorStrategy = local.supervisorStrategy

  override def withFallback(other: RouterConfig): RouterConfig = other match {
    case ClusterRouterConfig(_: ClusterRouterConfig, _) ⇒ throw new IllegalStateException(
      "ClusterRouterConfig is not allowed to wrap a ClusterRouterConfig")
    case ClusterRouterConfig(local, _) ⇒
      // FIXME #3549 is this always correct?
      copy(local = this.local.withFallback(local).asInstanceOf[DeprecatedRouterConfig])
    case _ ⇒
      copy(local = this.local.withFallback(other).asInstanceOf[DeprecatedRouterConfig])
  }

}

/**
 * INTERNAL API
 */
private[akka] class ClusterPoolActor(
  supervisorStrategy: SupervisorStrategy, val settings: ClusterRouterSettings)
  extends RouterPoolActor(supervisorStrategy) with ClusterRouterActor {

  override def receive = clusterReceive orElse super.receive

  /**
   * Adds routees based on totalInstances and maxInstancesPerNode settings
   */
  def addRoutees(): Unit = {
    @tailrec
    def doAddRoutees(): Unit = selectDeploymentTarget match {
      case None ⇒ // done
      case Some(target) ⇒
        val routeeProps = cell.routeeProps
        val deploy = Deploy(config = ConfigFactory.empty(), routerConfig = routeeProps.routerConfig,
          scope = RemoteScope(target))
        val routee = pool.newRoutee(routeeProps.withDeploy(deploy), context)
        // must register each one, since registered routees are used in selectDeploymentTarget
        cell.addRoutee(routee)

        // recursion until all created
        doAddRoutees()
    }

    doAddRoutees()
  }

}

/**
 * INTERNAL API
 */
private[akka] class ClusterNozzleActor(val settings: ClusterRouterSettings)
  extends RouterActor with ClusterRouterActor {

  val nozzle = cell.routerConfig match {
    case x: Nozzle ⇒ x
    case other ⇒
      throw ActorInitializationException("ClusterNozzleActor can only be used with Nozle, not " + other.getClass)
  }

  override def receive = clusterReceive orElse super.receive

  /**
   * Adds routees based on totalInstances and maxInstancesPerNode settings
   */
  def addRoutees(): Unit = {
    @tailrec
    def doAddRoutees(): Unit = selectDeploymentTarget match {
      case None ⇒ // done
      case Some(target) ⇒
        val routee = nozzle.routeeFor(target + settings.routeesPath, context)
        // must register each one, since registered routees are used in selectDeploymentTarget
        cell.addRoutee(routee)

        // recursion until all created
        doAddRoutees()
    }

    doAddRoutees()
  }

}

/**
 * INTERNAL API
 * The router actor, subscribes to cluster events and
 * adjusts the routees.
 */
private[akka] trait ClusterRouterActor { this: RouterActor ⇒

  def settings: ClusterRouterSettings

  if (!cell.routerConfig.isInstanceOf[Pool] && !cell.routerConfig.isInstanceOf[Nozzle])
    throw ActorInitializationException("Cluster router actor can only be used with Pool or Nozzle, not with " +
      cell.routerConfig.getClass)

  def cluster: Cluster = Cluster(context.system)

  // re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
    cluster.subscribe(self, classOf[ReachabilityEvent])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  var nodes: immutable.SortedSet[Address] = {
    import Member.addressOrdering
    cluster.readView.members.collect {
      case m if isAvailable(m) ⇒ m.address
    }
  }

  def isAvailable(m: Member): Boolean =
    m.status == MemberStatus.Up &&
      satisfiesRole(m.roles) &&
      (settings.allowLocalRoutees || m.address != cluster.selfAddress)

  private def satisfiesRole(memberRoles: Set[String]): Boolean = settings.useRole match {
    case None    ⇒ true
    case Some(r) ⇒ memberRoles.contains(r)
  }

  def availableNodes: immutable.SortedSet[Address] = {
    import Member.addressOrdering
    val currentNodes = nodes
    if (currentNodes.isEmpty && settings.allowLocalRoutees && satisfiesRole(cluster.selfRoles))
      //use my own node, cluster information not updated yet
      immutable.SortedSet(cluster.selfAddress)
    else
      currentNodes
  }

  /**
   * Fills in self address for local ActorRef
   */
  def fullAddress(routee: Routee): Address = {
    val a = routee match {
      case ActorRefRoutee(ref)       ⇒ ref.path.address
      case ActorSelectionRoutee(sel) ⇒ sel.anchor.path.address
    }
    a match {
      case Address(_, _, None, None) ⇒ cluster.selfAddress
      case a                         ⇒ a
    }
  }

  /**
   * Adds routees based on totalInstances and maxInstancesPerNode settings
   */
  def addRoutees(): Unit

  def selectDeploymentTarget: Option[Address] = {
    val currentRoutees = cell.router.routees
    val currentNodes = availableNodes
    if (currentNodes.isEmpty || currentRoutees.size >= settings.totalInstances) {
      None
    } else {
      // find the node with least routees
      val numberOfRouteesPerNode: Map[Address, Int] =
        currentRoutees.foldLeft(currentNodes.map(_ -> 0).toMap.withDefaultValue(0)) { (acc, x) ⇒
          val address = fullAddress(x)
          acc + (address -> (acc(address) + 1))
        }

      val (address, count) = numberOfRouteesPerNode.minBy(_._2)
      if (count < settings.maxInstancesPerNode) Some(address) else None
    }
  }

  def addMember(member: Member) = {
    nodes += member.address
    addRoutees()
  }

  def removeMember(member: Member) = {
    val address = member.address
    nodes -= address

    // unregister routees that live on that node
    val affectedRoutees = cell.router.routees.filter(fullAddress(_) == address)
    cell.removeRoutees(affectedRoutees, stopChild = true)

    // addRoutees will not create more than createRoutees and maxInstancesPerNode
    // this is useful when totalInstances < upNodes.size
    addRoutees()
  }

  def clusterReceive: Receive = {
    case s: CurrentClusterState ⇒
      import Member.addressOrdering
      nodes = s.members.collect { case m if isAvailable(m) ⇒ m.address }
      addRoutees()

    case m: MemberEvent if isAvailable(m.member) ⇒
      addMember(m.member)

    case other: MemberEvent ⇒
      // other events means that it is no longer interesting, such as
      // MemberExited, MemberRemoved
      removeMember(other.member)

    case UnreachableMember(m) ⇒
      removeMember(m)

    case ReachableMember(m) ⇒
      if (isAvailable(m)) addMember(m)
  }
}

