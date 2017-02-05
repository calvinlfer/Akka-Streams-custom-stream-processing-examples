package com.calvin.streamy

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}


class NumbersSource extends GraphStage[SourceShape[Int]] {
  // the outlet port of this stage which produces Ints
  val out: Outlet[Int] = Outlet("NumbersSource")

  // define the shape of this stage, we want it to be a source
  override val shape: SourceShape[Int] = SourceShape(out)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
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
      })
    }
}

