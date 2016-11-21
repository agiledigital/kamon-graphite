package kamon.graphite

import java.util.regex.Pattern

import akka.util.ByteString

/**
  * Encodes metrics using the graphite plaintext protocol.
  *
  * @see http://graphite.readthedocs.io/en/latest/feeding-carbon.html
  */
class GraphitePlaintextEncoder {

  import GraphitePlaintextEncoder._

  /**
    * Encodes a metric (name, value and time) in the graphite plaintext protocol.
    *
    * @param name  the name of the metric to send.
    * @param value the value to send.
    * @param time  the time at which the value was recorded.
    */
  def encode(name: String, value: String, time: Long): ByteString = {
    val buf = new StringBuilder
    buf.append(sanitize(name))
    buf.append(' ')
    buf.append(sanitize(value))
    buf.append(' ')
    buf.append(time)
    buf.append('\n')
    ByteString(buf.toString)
  }

  private def sanitize(s: String): String = WHITESPACE.matcher(s).replaceAll("-")
}

/**
  * Companion object containing constants.
  */
object GraphitePlaintextEncoder {
  private val WHITESPACE: Pattern = Pattern.compile("[\\s]+")
}
