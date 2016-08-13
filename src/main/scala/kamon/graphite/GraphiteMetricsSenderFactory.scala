package kamon.graphite

import akka.actor.Props
import com.typesafe.config.Config

trait GraphiteMetricsSenderFactory {
  def props(graphiteConfig: Config, metricKeyGenerator: MetricKeyGenerator): Props
}
