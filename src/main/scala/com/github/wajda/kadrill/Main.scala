package com.github.wajda.kadrill

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import scopt.OParser

import java.util.{Collections, Properties}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@main
def Main(args: String*): Unit =
  val builder = OParser.builder[Config]
  val parser = {
    import builder.*
    OParser.sequence(
      programName("kafka-drill"),
      opt[String]('t', "topic")
        .required()
        .action((x, c) => c.copy(topic = x))
        .text("topic name"),
    )
  }

  OParser.parse(parser, args, Config()) match {
    case Some(config) => exec(config)
    case _ => // arguments are bad, usage message will have been displayed
  }

def exec(config: Config): Unit =
  Console.println(
    s"""
       |Kafka Drill
       |-------------------------------------------------------------
       |Bootstrap servers   : ${config.props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)}
       |Kafka Topic Name    : ${config.topic}
       |Consumer Group Name : ${config.props.get(ConsumerConfig.GROUP_ID_CONFIG)}
       |-------------------------------------------------------------       |
       |""".stripMargin
  )

  val producer = new KafkaProducer[String, String](config.props)
  val consumer = new KafkaConsumer[String, String](config.props)

  val eventuallyProduced = Future(produce(producer, config))
  val eventuallyConsumed = Future(consume(consumer, config))

  val eventuallyDone = for {
    _ <- eventuallyProduced
    _ <- eventuallyConsumed
  } yield {
    Console.println("DONE")
  }

  Await.result(eventuallyDone, Duration.Inf)

def produce(producer: KafkaProducer[String, String], config: Config): Unit =
  Console.println("Start sending...")
  (1 to Config.N).foreach { i =>
    val msgKey = s"${Config.AppID}::$i"
    val record = new ProducerRecord(config.topic, msgKey, s"ID: $msgKey: dummy message $i")
    producer.send(record).get()
  }
  Console.println("...Sending complete")
  producer.close()

def consume(consumer: KafkaConsumer[String, String], config: Config): Unit =
  Console.println("Start receiving...")
  consumer.subscribe(Collections.singletonList(config.topic))
  var i = 0
  while (i < Config.N) {
    val records = consumer.poll(java.time.Duration.ofSeconds(1)).iterator().asScala
    for (record <- records) {
      val key = record.key()
      if (!key.startsWith(Config.AppID))
        Console.println(s"Skipping key: $key")
      else
        println(s"Received message: ${record.value()} at offset ${record.offset()}")
        i += 1
    }
    Console.println(s"loop: i=$i")
  }
