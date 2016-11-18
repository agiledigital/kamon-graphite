package kamon.graphite

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }
import akka.io.Udp.Send
import akka.io.{ IO, Udp }
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
  override def props(graphiteConfig: Config, metricKeyGenerator: MetricKeyGenerator): Props = {
    Props(new UDPBasedGraphiteMetricsSender(graphiteConfig, metricKeyGenerator))
  }
}

class UDPBasedGraphiteMetricsSender(graphiteConfig: Config, metricKeyGenerator: MetricKeyGenerator)
    extends Actor with UdpExtensionProvider with ActorLogging {

  type GraphiteMetric = (String, String)

  import collection.JavaConverters._

  private val graphiteHost = graphiteConfig.getString("hostname")
  private val graphitePort = graphiteConfig.getInt("port")
  private val percentiles: Seq[Integer] = Some(graphiteConfig.getIntList("percentiles")).map(_.asScala).getOrElse(Nil)

  private val plaintextEncoder = new GraphitePlaintextEncoder()

  udpExtension(context.system) ! Udp.SimpleSender

  private var udpSender: Option[ActorRef] = None

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.debug(s"Sending PoisonPill to [$udpSender] during restart.")
    udpSender.foreach(_ ! PoisonPill)
  }

  def receive: Receive = {
    case Udp.SimpleSenderReady =>
      this.udpSender = Some(sender())
      context.become(ready(sender()))
  }

  def ready(udp: ActorRef): Receive = {
    case tick: TickMetricSnapshot => writeMetricsToRemote(tick, udp)
  }

  /**
    * Writes the metrics in the metric snapshot to the remote host using the UDP actor.
    *
    * @param tick the metrics to write.
    * @param udp  the UDP actor to send the metrics to for delivery.
    */
  private def writeMetricsToRemote(tick: TickMetricSnapshot, udp: ActorRef): Unit = {

    val time: Long = tick.from.toTimestamp.seconds

    val address = new InetSocketAddress(graphiteHost, graphitePort)

    val sendMetric: (GraphiteMetric) => Unit = send(time, udp, address)

    for (
      (entity, snapshot) <- tick.metrics
    ) {
      snapshot.histograms.foreach {
        case (metricKey, hs) =>
          sendHistogram(time, entity, metricKey, hs)(sendMetric)
      }

      snapshot.counters.foreach {
        case (metricKey, cs) =>
          sendCounter(time, entity, metricKey, cs)(sendMetric)
      }

      snapshot.gauges.foreach {
        case (metricKey, gauge) =>
          sendGauge(time, entity, metricKey, gauge)(sendMetric)
      }

      snapshot.minMaxCounters.foreach {
        case (metricKey, cs) =>
          sendMinMaxCounter(time, entity, metricKey, cs)(sendMetric)
      }
    }
  }

  /**
    * Sends the metric to the specified address using the provided actor.
    *
    * @param time    the time at which the metric was recorded.
    * @param udp     the actor that will be sent the metric for delivery.
    * @param address the address to which the metric will be sent.
    * @param metric  the metric to send.
    */
  private def send(time: Long, udp: ActorRef, address: InetSocketAddress)(metric: GraphiteMetric): Unit = {
    val byteString = plaintextEncoder.encode(metric._1, metric._2, time)
    val send = Send(byteString, address)
    udp ! send
  }

  private def sendMinMaxCounter(time: Long, entity: Entity, metricKey: MinMaxCounterKey, cs: HistogramSnapshot)(sendMetric: (GraphiteMetric) => Unit): Unit = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
    val mean = if (cs.numberOfMeasurements == 0) 0 else cs.sum / cs.numberOfMeasurements

    sendMetric(keyPrefix + ".upper" -> cs.max.toString)
    sendMetric(keyPrefix + ".lower" -> cs.min.toString)
    sendMetric(keyPrefix + ".mean" -> mean.toString)
  }

  private def sendGauge(time: Long, entity: Entity, metricKey: GaugeKey, gauge: HistogramSnapshot)(sendMetric: (GraphiteMetric) => Unit): Unit = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
    val mean = if (gauge.numberOfMeasurements == 0) 0 else gauge.sum / gauge.numberOfMeasurements

    sendMetric(keyPrefix + ".upper" -> gauge.max.toString)
    sendMetric(keyPrefix + ".lower" -> gauge.min.toString)
    sendMetric(keyPrefix + ".sum" -> gauge.sum.toString)
    sendMetric(keyPrefix + ".median" -> gauge.percentile(50D).toString)
    sendMetric(keyPrefix + ".mean" -> mean.toString)

    percentiles.foreach { percentile ⇒
      sendMetric(keyPrefix + s".upper_$percentile" -> gauge.percentile(percentile.doubleValue()).toString)
    }
  }

  private def sendCounter(time: Long, entity: Entity, metricKey: CounterKey, cs: CounterSnapshot)(sendMetric: (GraphiteMetric) => Unit): Unit = {
    val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)

    sendMetric(keyPrefix -> cs.count.toString)
  }

  private def sendHistogram(time: Long, entity: Entity, metricKey: HistogramKey, hs: HistogramSnapshot)(sendMetric: (GraphiteMetric) => Unit): Unit = {

    if (hs.numberOfMeasurements == 0) {
      ()
    }
    else {
      val keyPrefix = metricKeyGenerator.generateKey(entity, metricKey)
      val mean = if (hs.numberOfMeasurements == 0) 0 else hs.sum / hs.numberOfMeasurements

      sendMetric(keyPrefix + ".count" -> hs.numberOfMeasurements.toString)
      sendMetric(keyPrefix + ".upper" -> hs.max.toString)
      sendMetric(keyPrefix + ".lower" -> hs.min.toString)
      sendMetric(keyPrefix + ".sum" -> hs.sum.toString)
      sendMetric(keyPrefix + ".median" -> hs.percentile(50D).toString)
      sendMetric(keyPrefix + ".mean" -> mean.toString)

      percentiles.foreach { percentile ⇒
        sendMetric(keyPrefix + s".upper_$percentile" -> hs.percentile(percentile.doubleValue()).toString)
      }
    }
  }
}

trait UdpExtensionProvider {
  def udpExtension(implicit system: ActorSystem): ActorRef = IO(Udp)
}
