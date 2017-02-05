package com.calvin.streamy

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

object Example extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)

  // 5050
  val result1: Future[Int] = source.take(100).runFold(0)(_ + _)
  println {
    Await.result(result1, 2 seconds)
  }

  // 5050
  val result2: Future[Int] = source.take(100).runFold(0)(_ + _)
  println {
    Await.result(result2, 2 seconds)
  }
}

object Example2 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  source.to(sink).run()
}

object Example3 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  def mapFlowGraph[A, B](f: A => B): Graph[FlowShape[A, B], NotUsed] = new MapperFlow[A, B](f)

  val timesTenFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(mapFlowGraph(i => i * 10))

  source.take(10).via(timesTenFlow).to(sink).run()
}

object Example4 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  def mapFlowGraph[A, B](f: A => B): Graph[FlowShape[A, B], NotUsed] = new MapperFlow[A, B](f)
  def filterFlowGraph[A](pred: A => Boolean): Graph[FlowShape[A, A], NotUsed] = new FilterFlow[A](pred)

  val timesTenFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(mapFlowGraph(i => i * 10))
  val elemsEqualsAndOverEightyFilter: Flow[Int, Int, NotUsed] = Flow.fromGraph(filterFlowGraph(p => p >= 80))

  source.take(10).via(timesTenFlow).via(elemsEqualsAndOverEightyFilter).to(sink).run()
}

object Example5 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  def duplicatorGraph[A]: Graph[FlowShape[A, A], NotUsed] = new DuplicatorFlow[A]
  val duplicatorFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(duplicatorGraph)

  source.take(10).via(duplicatorFlow).to(sink).run()
}

object Example6 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  def duplicatorV2Graph[A]: Graph[FlowShape[A, A], NotUsed] = new DuplicatorV2Flow[A]
  val duplicatorFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(duplicatorV2Graph)

  source.take(10).via(duplicatorFlow).to(sink).run()
}

object FullPipelineExample extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  Source.fromGraph(new NumbersSource)
      .take(10)
      .via(new FilterFlow[Int](x => x % 2 == 0))
      .via(new DuplicatorV2Flow[Int])
      .via(new MapperFlow(x => x / 2))
      .runWith(new StdOutSink)
}

object Example7 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new NumbersSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  def circuitBreakerGraph[A](duration: FiniteDuration): Graph[FlowShape[A, A], NotUsed] = new CircuitBreakerFlow[A](duration)
  val circuitBreakerFlow: Flow[Int, Int, NotUsed] = Flow.fromGraph(circuitBreakerGraph(5 seconds))

  source.via(circuitBreakerFlow).to(sink).run()
}

object Example8 extends App {
  implicit val system = ActorSystem("graphstage-experimentation")
  implicit val mat = ActorMaterializer()

  val sourceGraph: Graph[SourceShape[Int], NotUsed] = new SideChannelSource
  val source: Source[Int, NotUsed] = Source.fromGraph(sourceGraph)
  val sinkGraph: Graph[SinkShape[Int], NotUsed] = new StdOutSink
  val sink: Sink[Int, NotUsed] = Sink.fromGraph(sinkGraph)

  source.to(sink).run()
}