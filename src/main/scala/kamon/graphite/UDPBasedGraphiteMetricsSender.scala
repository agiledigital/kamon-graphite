package kamon.graphite

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.io.{ IO, Udp }
import com.codahale.metrics.graphite._
import com.typesafe.config.Config
import kamon.metric.SubscriptionsDispatcher.TickMetricSnapshot
import kamon.metric._
import kamon.metric.instrument.Counter.{ Snapshot => CounterSnapshot }
import kamon.metric.instrument.Histogram.{ Snapshot => HistogramSnapshot }

/**
  * Factory for [[UDPBasedGraphiteMetricsSender]].
  * Use FQCN of the object in "kamon.graphite.graphite-metrics-sender"
  * to select [[UDPBasedGraphiteMetricsSender]] as your sender
  */
object UDPBasedGraphiteMetricsSender extends GraphiteMetricsSenderFactory {
  override def props(graphiteConfig: Config, metricKeyGenerator: MetricKeyGenerator): Props =
    Props(new UDPBasedGraphiteMetricsSender(graphiteConfig, metricKeyGenerator))
}

class UDPBasedGraphiteMetricsSender(graphiteConfig: Config, metricKeyGenerator: MetricKeyGenerator)
    extends Actor with UdpExtensionProvider with ActorLogging {

  type GraphiteMetric = (String, String)

  import collection.JavaConverters._

  val graphiteHost = graphiteConfig.getString("hostname")
  val graphitePort = graphiteConfig.getInt("port")
  val percentiles: Seq[Integer] = Some(graphiteConfig.getIntList("percentiles")).map(_.asScala).getOrElse(Nil)

  val graphite = new GraphiteUDP(graphiteHost, graphitePort)

  def receive: Receive = {
    case tick: TickMetricSnapshot => writeMetricsToRemote(tick)
  }

  def writeMetricsToRemote(tick: TickMetricSnapshot): Unit = {
    val time = tick.from.toTimestamp.seconds

    for (
      (entity, snapshot) <- tick.metrics
    ) {
      val metrics = snapshot.histograms.flatMap {
        case (metricKey, hs) =>
          writeHistogram(time, entity, metricKey, hs)
      } ++ snapshot.counters.flatMap {
        case (metricKey, cs) =>
          writeCounter(time, entity, metricKey, cs)
      } ++ snapshot.gauges.flatMap {
        case (metricKey, gauge) =>
          writeGauge(time, entity, metricKey, gauge)
      } ++ snapshot.minMaxCounters.flatMap {
        case (metricKey, cs) =>
          writeMinMaxCounter(time, entity, metricKey, cs)
      }

      metrics.foreach {
        case (name, value) =>
          graphite.send(name, value, time)
      }

      val failures = graphite.getFailures
      if (failures > 0) {
        log.warning(s"Failed to send [$failures] / [${metrics.size}] metrics to [$graphiteHost]:[$graphitePort].")
      }
    }
  }

  private def writeMinMaxCounter(time: Long, entity: Entity, metricKey: MinMaxCounterKey, cs: HistogramSnapshot): Seq[GraphiteMetric] = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
    val mean = if (cs.numberOfMeasurements == 0) 0 else cs.sum / cs.numberOfMeasurements

    Seq(
      keyPrefix + ".upper" -> cs.max.toString,
      keyPrefix + ".lower" -> cs.min.toString,
      keyPrefix + ".mean" -> mean.toString
    )
  }

  private def writeGauge(time: Long, entity: Entity, metricKey: GaugeKey, gauge: HistogramSnapshot): Seq[GraphiteMetric] = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
    val mean = if (gauge.numberOfMeasurements == 0) 0 else gauge.sum / gauge.numberOfMeasurements

    Seq(
      keyPrefix + ".upper" -> gauge.max.toString,
      keyPrefix + ".lower" -> gauge.min.toString,
      keyPrefix + ".sum" -> gauge.sum.toString,
      keyPrefix + ".median" -> gauge.percentile(50D).toString,
      keyPrefix + ".mean" -> mean.toString
    ) ++
      percentiles.map { percentile ⇒
        keyPrefix + s".upper_$percentile" -> gauge.percentile(percentile.doubleValue()).toString
      }
  }

  private def writeCounter(time: Long, entity: Entity, metricKey: CounterKey, cs: CounterSnapshot): Seq[GraphiteMetric] = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)

    Seq(keyPrefix -> cs.count.toString)
  }

  private def writeHistogram(time: Long, entity: Entity, metricKey: HistogramKey, hs: HistogramSnapshot): Seq[GraphiteMetric] = {

    if (hs.numberOfMeasurements == 0) {
      Nil
    }
    else {
      val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
      val mean = if (hs.numberOfMeasurements == 0) 0 else hs.sum / hs.numberOfMeasurements

      Seq(
        keyPrefix + ".count" -> hs.numberOfMeasurements.toString,
        keyPrefix + ".upper" -> hs.max.toString,
        keyPrefix + ".lower" -> hs.min.toString,
        keyPrefix + ".sum" -> hs.sum.toString,
        keyPrefix + ".median" -> hs.percentile(50D).toString,
        keyPrefix + ".mean" -> mean.toString
      ) ++
        percentiles.map { percentile =>
          keyPrefix + s".upper_$percentile" -> hs.percentile(percentile.doubleValue()).toString
        }
    }
  }
}

trait UdpExtensionProvider {
  def udpExtension(implicit system: ActorSystem): ActorRef = IO(Udp)
}
