package com.calvin.streamy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

class DuplicatorV2Flow[A] extends GraphStage[FlowShape[A, A]] {

  val inlet: Inlet[A] = Inlet[A]("MapperFlow.input")
  val outlet: Outlet[A] = Outlet[A]("MapperFlow.output")

  override def shape: FlowShape[A, A] = FlowShape.of(inlet, outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      /*
             upstream                         downstream

            ----inlet--> Our GraphStageLogic ---outlet->
           we get pushed                      we get pulled
           from the inlet                     from the outlet
       */

      // when you are pushed data by the upstream
      setHandler(inlet, new InHandler {
        @scala.throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val obtainedValue = grab(inlet)
          // emitMultiple instead of push so it takes care of the dance since we are pushing multiple elements
          emitMultiple(outlet, List.fill(2)(obtainedValue))
        }
      })

      setHandler(outlet, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        // when you are pulled by the downstream
        override def onPull(): Unit = {
          pull(inlet)
        }
      })
    }
}
