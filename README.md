# Custom Stream Processing in Akka Streams with GraphStages
Demos of how to do custom stream processing using the Akka Streams GraphStages API

## Dealing with asynchronous channels
You would use this kind of custom stream processing when you have to deal with integrating with external parties like 
Amazon's SQS, polling an HTTP endpoint (take a look at timers if you are getting throttled), etc.

From the [documentation](http://doc.akka.io/docs/akka/2.4/scala/stream/stream-customize.html?_ga=1.95210907.668506683.1483746547#Using_asynchronous_side-channels)

*In order to receive asynchronous events that are not arriving as stream elements (for example a completion of a future 
or a callback from a 3rd party API) one must acquire a `AsyncCallback` by calling `getAsyncCallback()` from the stage 
logic. The method `getAsyncCallback` takes as a parameter a callback that will be called once the asynchronous event 
fires. **It is important to not call the callback directly**, instead, the external API must call the `invoke(event)` 
method on the returned `AsyncCallback`. The execution engine will take care of calling the provided callback in a 
**thread-safe** way. The callback can safely access the state of the **GraphStageLogic** implementation.*

The documentation provides an example but here's something slightly more complex.

### Problem statement
Create a Random Numbers `Source` where in order to get a random number, you have to poll an API (we pretend to do this) 
and the result is in a `Future`. As an added twist, the API could fail and we want to retry after X seconds. 

### Solution
The documentation says you need an `AsyncCallback`. When your asynchronous call gets a result, it needs to call `invoke` 
with the result on this `AsyncCallback`. They also mention that the `AsyncCallback` takes a function as a parameter. 
It will call the function with the result of the asynchronous call in a thread safe way. In our code, we will call this 
the target handler. They also recommend that you set all this up in the `preStart` hook.

![image](https://cloud.githubusercontent.com/assets/14280155/22624572/10a9ed9a-eb4e-11e6-9340-329dd623288f.png)

Here `bufferMessageAndEmulatePull` is our target handler. As you can see, when the target handler gets a message, it 
adds it to the buffer and emulates a pull as if the downstream is calling. Your first reaction is correct in saying that 
it is unsafe to do this which is why in the handler we will check whether we were truly called using the query API to do 
so. Doing this actually allows us to write less code since the checks are in a single area.

We have setup our `AsyncHandler` so that when it is called with the results, it calls our target handler with the results 
in a thread safe way.

The last thing that is left to do is actually make the asynchronous call and when the call completes to call `invoke` on 
our `AsyncHandler` so we can get the results safely.

![image](https://cloud.githubusercontent.com/assets/14280155/22624583/4ae4f22a-eb4e-11e6-936e-1dcb61e6bd86.png)

Let's walk through those functions

![image](https://cloud.githubusercontent.com/assets/14280155/22624586/5a7acb9c-eb4e-11e6-91d5-7d6fc1c442ba.png)

I said our asynchronous call could fail by throwing an exception (insert cry here) or it produces a result. Here is a 
synchronous call that produces numbers and we wrap it in a `Future` to make it asynchronous.

So where are we going to call `invoke` then? 

![image](https://cloud.githubusercontent.com/assets/14280155/22624599/a5c2f7e6-eb4e-11e6-99d8-12547754bf73.png)

Here's where we listen for completion of the asynchronous call, get the result and call `invoke`. Remember I also said 
I wanted to retry after X seconds (2 seconds for now), so we do that here.

With all this in place we are ready to write our `OutHandler`.

![image](https://cloud.githubusercontent.com/assets/14280155/22624602/e7e46646-eb4e-11e6-874d-63baa1a7f7f5.png)

As you can see, when we are pulled we have to first check whether we are truly pulled from the downstream or whether we 
are artificially called from `bufferMessageAndEmulatePull`. So we use the query `isAvailable` on the `outlet` port to 
check.

- If we are truly pulled then we take the first element off the buffer and send it downstream using `push`. 

- If we have run out of elements in the buffer then we make our asynchronous call which involves taking the results and 
obtaining them in a thread safe way.

You can find the entire picture [here](https://github.com/calvinlfer/Akka-Streams-custom-stream-processing-examples/blob/master/src/main/scala/com/calvin/streamy/SideChannelSource.scala)

You can find it in action [here](https://github.com/calvinlfer/Akka-Streams-custom-stream-processing-examples/blob/master/src/main/scala/com/calvin/streamy/Example.scala#L135)

The example will keep producing random numbers and handle retries internally in the midst of failures that could occur.

