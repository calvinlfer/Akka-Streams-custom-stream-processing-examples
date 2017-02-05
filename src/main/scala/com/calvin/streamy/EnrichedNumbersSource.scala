package com.calvin.streamy

import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}

import scala.concurrent.{Future, Promise}

/**
  * A Numbers Source that also produces a materialization value
  */
class EnrichedNumbersSource extends GraphStageWithMaterializedValue[SourceShape[Int], Future[Int]] {
  // the outlet port of this stage which produces Ints
  val out: Outlet[Int] = Outlet("NumbersSource")

  // define the shape of this stage, we want it to be a source
  override val shape: SourceShape[Int] = SourceShape(out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
    val promise: Promise[Int] = Promise[Int]()
    val graphStageLogic = new GraphStageLogic(shape) with StageLogging {
      // mutable state must be inside the GraphStageLogic and never outside
      private var counter = 1

      // create a handler for the outlet
      setHandler(out, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        // when you are asked for data (pulled for data)
        override def onPull(): Unit = {
          // emit an element on the outlet
          push(out, counter)

          // increment internal state
          log.debug(s"Incrementing counter: {}", counter)
          counter += 1
        }

        // called when the downstream doesn't want any more elements
        @scala.throws[Exception](classOf[Exception])
        override def onDownstreamFinish(): Unit = {
          // fill in the promise that is used as the materialized value
          promise.success(counter)
          super.onDownstreamFinish()
        }
      })
    }
    (graphStageLogic, promise.future)
  }
}

