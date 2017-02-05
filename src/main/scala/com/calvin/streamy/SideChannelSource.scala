package com.calvin.streamy

import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success}

class SideChannelSource extends GraphStage[SourceShape[Int]] {

  val outlet: Outlet[Int] = Outlet[Int]("SideChannel.out")

  override def shape: SourceShape[Int] = SourceShape(outlet)

  @scala.throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      var callback: AsyncCallback[Int] = _
      var buffer: Vector[Int] = Vector.empty
      var asyncCallInProgress = false

      // grab the result of the asynchronous call and invoke the safe callback
      // also performs 2 second retries
      private def grabAndInvokeWithRetry(future: Future[Int]): Unit = {
        asyncCallInProgress = true
        future.onComplete {
          case Success(randInt) =>
            callback.invoke(randInt)

          case Failure(ex) =>
            log.error(ex, "Error occurred in SideChannelSource")
            log.info("Attempting again after 2 seconds")
            materializer.scheduleOnce(2 seconds, new Runnable {
              override def run(): Unit = grabAndInvokeWithRetry(future)
            })
        }(materializer.executionContext)
      }

      @scala.throws[Exception](classOf[Exception])
      private def dangerousComputation(): Int = {
        val next = scala.util.Random.nextInt(100)
        if (next < 10) throw new Exception("Number is below 10") with NoStackTrace
        else next
      }

      override def preStart(): Unit = {
        // Setup safe callback and its target handler

        // target handler of getAsyncCallback, this function will be called
        // when the side channel has data
        def bufferMessageAndEmulatePull(incoming: Int): Unit = {
          asyncCallInProgress = false
          buffer = buffer :+ incoming
          // emulate downstream asking for data by calling onPull on the outlet port
          // Note: we check whether the downstream is really asking there
          getHandler(outlet).onPull()
        }

        // In order to receive asynchronous events that are not arriving as stream elements
        // (for example a completion of a future or a callback from a 3rd party API) one must acquire a
        // AsyncCallback by calling getAsyncCallback() from the stage logic.
        // The method getAsyncCallback takes as a parameter a callback that will be called once the
        // asynchronous event fires.
        // This is a proxy that that asynchronous side channel needs to call for safety
        // this proxy delegates the data obtained from the callback to bufferMessageAndEmulatePull
        callback = getAsyncCallback[Int](bufferMessageAndEmulatePull)

        // emulate fake asynchronous call whose results we must obtain (which invokes the above callback)
        grabAndInvokeWithRetry(Future(dangerousComputation())(materializer.executionContext))
      }

      setHandler(outlet, new OutHandler {
        @scala.throws[Exception](classOf[Exception])
        // the downstream will pull us
        override def onPull(): Unit = {
          // we query here because bufferMessageAndEmulatePull artificially calls onPull
          // and we must not violate the GraphStages guarantees
          if (buffer.nonEmpty && isAvailable(outlet)) {
            val sendValue = buffer.head
            buffer = buffer.drop(1)
            push(outlet, sendValue)
          }

          // obtain more elements if the buffer is empty
          if (buffer.isEmpty) {
            grabAndInvokeWithRetry(Future(dangerousComputation())(materializer.executionContext))
          }
        }
      })
    }
}

