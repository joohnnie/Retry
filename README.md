# Retry

A lightweight and flexible Scala library for handling retries in synchronous and asynchronous functions
in case of failure.

The Retry library provides three retry strategies: fixed delay, exponential backoff, and random delay. 
You can choose the strategy that works best for your use case.

## Features

- Supports both synchronous and asynchronous functions
- Customizable retry strategies with fixed delay, exponential backoff, and random delay options
- Configurable retry conditions with non-retryable exceptions and max retries
- Easy to use and integrate into your Scala projects

## Installation

Add the following dependency to your `build.sbt` file:

```scala
libraryDependencies += "fun.zyx" %% "retry" % "1.0.0"
```
## Usage

First, import the Retry library

```
import fun.zyx.retry._

```


## Synchronous Function
To use the retry library with synchronous functions, wrap your function with the retry method:

```
import fun.zyx.retry._
import scala.concurrent.duration._

val result = Retry.retry(RetryStrategy.fixedDelay(1.second)) {
  // Your code that may fail
}

```

## Asynchronous Functions

To use the retry library with asynchronous functions, wrap your function with the retryAsync method:

```
import scala.concurrent.duration._
import fun.zyx.retry._
import scala.concurrent.ExecutionContext.Implicits.global

val futureResult = Retry.retryAsync(RetryStrategy.fixedDelay(1.second)) {
  // Your code that may fail
}
```

## Retry Strategies

### Fixed Delay
The `fixedDelay` strategy retries the operation with a fixed delay between each retry attempt. 
To use this strategy, call `RetryStrategy.fixedDelay` and pass in the delay duration and the maximum number of retries. 
For example:

```scala
import fun.zyx.retry._

val strategy = RetryStrategy.fixedDelay(1.second, 3)

```

This will retry the operation with a 1-second delay between each retry, for up to 3 retries.

### Exponential Backoff

The `exponentialBackoff` strategy retries the operation with an exponentially increasing delay between each retry 
attempt. To use this strategy, call `RetryStrategy.exponentialBackoff` and pass in the initial delay duration 
and the maximum number of retries. For example:

```scala
import fun.zyx.retry._

val strategy = RetryStrategy.exponentialBackoff(1.second, 3)

```
This will retry the operation with an initial delay of 1 second, and then double the delay time after each retry, 
for up to 3 retries.


### Random Delay
The `randomDelay` strategy retries the operation with a random delay between each retry attempt. 
To use this strategy, call `RetryStrategy.randomDelay` and pass in the minimum delay duration, 
the maximum delay duration, and the maximum number of retries. For example:

```scala
import fun.zyx.retry._

val strategy = RetryStrategy.randomDelay(1.second, 10.seconds, 3)

```
This will retry the operation with a random delay between 1 second and 10 seconds between each retry, for up to 3 retries.


### Retry Exceptions
By default, the RetryStrategy will not retry when encountering exceptions of type `InterruptedException` or 
`IllegalArgumentException`. However, you can define a set of exceptions that 
should not be retried by creating a RetryStrategy object with a set of non-retryable exceptions. For example:

```scala
import fun.zyx.retry._

val strategy = RetryStrategy(
  shouldRetry = (retryCount, exception) => Some(1.second),
  nonRetryableExceptions = Set(classOf[IllegalArgumentException])
)

```

In this example, the Retry library will not retry any `IllegalArgumentException` exceptions.





## License
This project is licensed under the Apache-2.0 License.


