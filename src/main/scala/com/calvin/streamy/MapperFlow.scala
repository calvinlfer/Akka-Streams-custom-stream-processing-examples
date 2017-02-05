package com.calvin.streamy

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

class MapperFlow[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {

  val inlet: Inlet[A] = Inlet[A]("MapperFlow.input")
  val outlet: Outlet[B] = Outlet[B]("MapperFlow.output")

  override def shape: FlowShape[A, B] = FlowShape.of(inlet, outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // when you are pushed, you grab from the inlet, transform and push to the outlet
      // do not ask for more yet
      setHandler(inlet, new InHandler {
        @scala.throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val obtainedValue = grab(inlet)
          push(outlet, f(obtainedValue))
        }
      })

      setHandler(outlet, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        // when you are pulled by the downstream, you pull from the upstream (inlet)
        // which will cause the onPush of the inlet to trigger
        // that will accordingly take the element, transform and push it downstream on the outlet
        override def onPull(): Unit = {
          pull(inlet)
        }
      })
    }
}
