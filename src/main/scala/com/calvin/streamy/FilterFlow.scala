package com.calvin.streamy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

class FilterFlow[A](predicate: A => Boolean) extends GraphStage[FlowShape[A, A]] {

  val inlet: Inlet[A] = Inlet[A]("MapperFlow.input")
  val outlet: Outlet[A] = Outlet[A]("MapperFlow.output")

  override def shape: FlowShape[A, A] = FlowShape.of(inlet, outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // when you are pushed, you grab from the inlet
      // if the obtained value meets the predicate
      // -- it does so push it to the outlet
      // -- it doesn't so don't push it to outlet, instead pull the inlet again
      setHandler(inlet, new InHandler {
        @scala.throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val obtainedValue = grab(inlet)
          if (predicate(obtainedValue)) {
            push(outlet, obtainedValue)
          } else {
            pull(inlet)
          }
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
