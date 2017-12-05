# kamon-graphite

[![Build Status](https://travis-ci.org/agiledigital/kamon-graphite.svg?branch=master)](https://travis-ci.org/agiledigital/kamon-graphite)

A biased graphite backed for Kamon.

Emits aggregated stats with prefixes that match those that would have been produced by statsd.

Example configuration:
```
  graphite {
    hostname = "127.0.0.1"
    port = 2003

    # Interval between metrics data flushes to Graphite. It's value must be equal or greater than the
    # kamon.metrics.tick-interval setting.
    flush-interval = 10 seconds

    # Max packet size for UDP metrics data sent to Graphite.
    max-packet-size = 1024 bytes

    # Percentiles to report.
    percentiles = [5, 90, 95, 99]

    # Subscription patterns used to select which metrics will be pushed to Graphite. Note that first, metrics
    # collection for your desired entities must be activated under the kamon.metrics.filters settings.
    subscriptions {
      histogram = ["**"]
      min-max-counter = ["**"]
      gauge = ["**"]
      counter = ["**"]
      trace = ["**"]
      trace-segment = ["**"]
    }

    simple-metric-key-generator {
      # Application prefix for all metrics pushed to Graphite. The default namespacing scheme for metrics follows
      # this pattern:
      #    application.host.entity.entity-name.metric-name
      application = "core-services"
    }

    metric-key-generator = kamon.graphite.EscapingMetricKeyGenerator
  }
```
