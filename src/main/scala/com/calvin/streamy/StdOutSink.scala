package com.calvin.streamy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}

class StdOutSink extends GraphStage[SinkShape[Int]] {
  // the input port of the Sink which consumes Ints
  val in: Inlet[Int] = Inlet("StdOutSinkInput")

  override def shape: SinkShape[Int] = SinkShape(in)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // ask for an element as soon as you start
      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {
        @scala.throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          // grab the value from the buffer
          val obtainedValue = grab(in)
          // do operation
          println(obtainedValue)
          // ask for another
          pull(in)
        }
      })
    }
}
