package com.github.wajda.kadrill

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig

import java.util.function.Consumer
import java.util.{Properties, UUID}
import scala.jdk.CollectionConverters.*

case class Config
(
  topic: String = null,
  props: Properties = defaultProperties
)

object Config:
  val N: Int = 3
  val AppID: String = UUID.randomUUID().toString

  def defaultProperties: Properties = new Properties {
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    System.getProperties.entrySet().asScala.foreach { entry =>
      if (entry.getKey.toString.startsWith("kafka.")) {
        val k = entry.getKey.toString.drop("kafka.".length)
        val v = entry.getValue.toString
        setProperty(k, v)
      }
    }
  }
