import KafkaProducer.log
import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future
import scala.collection.mutable.{Map => MMap}

object Main extends App {

  implicit val system = ActorSystem("application")
  implicit val ec = system.dispatcher
  val topic = "topic"
  val consumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId("consumerGroup")
      .withBootstrapServers("0.0.0.0:9092")

  val commiterDefaults = CommitterSettings(system)

  println(s"""commiterDefaults $commiterDefaults""")

  implicit val producerSettings: ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("0.0.0.0:9092")

  KafkaProducer.produce((0 to 100).map(i => s"$i"), topic)(
    _.foreach(s => log.info(s"""Published $s to $topic"""))
  )

  case class State(out: String, committableOffset: CommittableOffset)
  val control =
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(topic))
      .batch[State](10L, msg => State("", msg.committableOffset)) { (out, msg) =>
        println(s"Processed ${msg.record.value}")
        State(out.out + "," + msg.record.value, msg.committableOffset)
      }
      .mapAsync(1) { msg: State =>
        business(msg).map(_ => msg.committableOffset)
      }
      .via(Committer.flow(commiterDefaults))
      .toMat(Sink.seq)(DrainingControl.apply)
      .run()

  case class SujetoId(id: String)
  case class ObjetoId(id: String)
  case class Sujeto(
      id: SujetoId,
      saldo: Int
  )
  case class Objeto(
      id: (SujetoId, ObjetoId),
      saldo: Int
  )

  case class Cotitularidad(
      objetoId: String,
      sujetoIds: List[String]
  )

  val sujetoProjection: MMap[(SujetoId), Int] = MMap.empty
  val objetoProjection: MMap[(SujetoId, ObjetoId), Int] = MMap.empty

  def business(state: State): Future[Done] = {
    Future {
      println(s"Saving entire state to Cassandra: ${state.out}")
      Thread.sleep(5000)
      println(s"Saved entire state to Cassandra: ${state.out}")
      Done
    }
  }

}
