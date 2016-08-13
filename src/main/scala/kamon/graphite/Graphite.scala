package kamon.graphite

import akka.actor._
import akka.event.Logging
import com.typesafe.config.Config
import kamon.Kamon
import kamon.metric._
import kamon.util.ConfigTools.Syntax
import kamon.util.NeedToScale

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object Graphite extends ExtensionId[GraphiteExtension] with ExtensionIdProvider {
  override def lookup(): ExtensionId[_ <: Extension] = Graphite
  override def createExtension(system: ExtendedActorSystem): GraphiteExtension = new GraphiteExtension(system)
}

class GraphiteExtension(system: ExtendedActorSystem) extends Kamon.Extension {
  implicit val as: ExtendedActorSystem = system

  val log = Logging(system, classOf[GraphiteExtension])
  log.info("Starting the Kamon(Graphite) extension")

  private val config = system.settings.config
  private val graphiteConfig = config.getConfig("kamon.graphite")
  val metricsExtension = Kamon.metrics

  val tickInterval = metricsExtension.settings.tickInterval
  val flushInterval = graphiteConfig.getFiniteDuration("flush-interval")
  val keyGeneratorFQCN = graphiteConfig.getString("metric-key-generator")
  val senderFactoryFQCN = graphiteConfig.getString("metric-sender-factory")

  val graphiteMetricsListener = buildMetricsListener(tickInterval, flushInterval, keyGeneratorFQCN, senderFactoryFQCN, config)

  val subscriptions = graphiteConfig.getConfig("subscriptions")
  subscriptions.firstLevelKeys.foreach { subscriptionCategory ⇒
    subscriptions.getStringList(subscriptionCategory).asScala.foreach { pattern ⇒
      metricsExtension.subscribe(subscriptionCategory, pattern, graphiteMetricsListener, permanently = true)
    }
  }

  def buildMetricsListener(tickInterval: FiniteDuration, flushInterval: FiniteDuration,
    keyGeneratorFQCN: String, senderFactoryFQCN: String, config: Config): ActorRef = {
    assert(flushInterval >= tickInterval, "graphite flush-interval needs to be equal or greater to the tick-interval")

    val actorRefT = for {
      keyGenerator <- system.dynamicAccess.createInstanceFor[MetricKeyGenerator](keyGeneratorFQCN, (classOf[Config], config) :: Nil)
      senderFactory <- system.dynamicAccess.getObjectFor[GraphiteMetricsSenderFactory](senderFactoryFQCN)
    } yield {

      val metricsSender = system.actorOf(senderFactory.props(graphiteConfig, keyGenerator), "graphite-metrics-sender")

      val decoratedSender = graphiteConfig match {
        case NeedToScale(scaleTimeTo, scaleMemoryTo) =>
          system.actorOf(MetricScaleDecorator.props(scaleTimeTo, scaleMemoryTo, metricsSender), "graphite-metric-scale-decorator")
        case _ => metricsSender
      }

      if (flushInterval == tickInterval) {
        // No need to buffer the metrics, let's go straight to the metrics sender.
        decoratedSender
      }
      else {
        system.actorOf(TickMetricSnapshotBuffer.props(flushInterval, decoratedSender), "graphite-metrics-buffer")
      }
    }

    actorRefT match {
      case Success(actorRef) => actorRef
      case Failure(t) => throw t
    }
  }
}
