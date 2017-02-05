package com.calvin.streamy

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

class CircuitBreakerFlow[A](silencePeriod: FiniteDuration) extends GraphStage[FlowShape[A, A]] {

  val inlet: Inlet[A] = Inlet[A]("Breaker.in")
  val outlet: Outlet[A] = Outlet[A]("Breaker.out")

  override def shape: FlowShape[A, A] = FlowShape(inlet, outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogicWithLogging(shape) {
      /*
        upstream                         downstream

        ----inlet--> Our GraphStageLogic ---outlet->
        we pull the                        we get onPull
        inlet and                          from the outlet
        we get onPush                      we then have to
        from the inlet                     push the outlet
      */

      val key = "circuit-breaker-closer"
      var open = false

      setHandler(inlet, new InHandler {
        // when pushed from the upstream, grab the value
        @scala.throws[Exception](classOf[Exception])
        override def onPush(): Unit = {
          val obtainedValue = grab(inlet)
          if (open) {
            // drop and ask for more from the upstream
            pull(inlet)
          } else {
            // send it downstream
            push(outlet, obtainedValue)
            log.debug("Opening breaker")
            open = true
            // set a timer to close it
            scheduleOnce(key, silencePeriod)
          }
        }
      })

      setHandler(outlet, new OutHandler {
        // when pulled by the downstream
        @scala.throws[Exception](classOf[Exception])
        override def onPull(): Unit = {
          // pull from the upstream
          pull(inlet)
        }
      })

      // triggered when scheduler finishes its timeout
      override protected def onTimer(timerKey: Any): Unit = timerKey match {
        case `key` =>
          log.debug("Closing breaker")
          open = false

        case other =>
          log.error(s"Warning: {} is not a valid key", other)
      }
    }
}
