package com.calvin.streamy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

class DuplicatorFlow[A] extends GraphStage[FlowShape[A, A]] {

  val inlet: Inlet[A] = Inlet[A]("MapperFlow.input")
  val outlet: Outlet[A] = Outlet[A]("MapperFlow.output")

  override def shape: FlowShape[A, A] = FlowShape.of(inlet, outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // mutable state
      var optLastSeen: Option[A] = None

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
          optLastSeen = Some(obtainedValue)
          // push value downstream for the first time
          push(outlet, obtainedValue)
        }

        // handle edge case if the upstream completes
        override def onUpstreamFinish(): Unit = {
          if (optLastSeen.isDefined)
            emit(outlet, optLastSeen.get)

          complete(outlet)
        }
      })

      setHandler(outlet, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        // when you are pulled by the downstream
        override def onPull(): Unit = {
          if (optLastSeen.isDefined) {
            // push element downstream for the second time
            push(outlet, optLastSeen.get)
            optLastSeen = None
          } else {
            // pull new element from the upstream
            pull(inlet)
          }
        }
      })
    }
}
