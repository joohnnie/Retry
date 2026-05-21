/*
 * Copyright 2023 Johnnie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fun.zyx.retry

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class RetrySpec extends AnyFlatSpec with Matchers {

  val customStrategy: RetryStrategy = RetryStrategy(
      shouldRetry = (retryCount, _) => if (retryCount < 3) Some(100.millis) else None
  )

  "retry" should "succeed after 2 retries" in {
    var attempts = 0
    val failingFunction: () => Int = () => {
      attempts += 1
      if (attempts < 3) throw new RuntimeException("Test failure") else 42
    }

    val result = Retry.retry(customStrategy)(failingFunction())
    result shouldEqual 42
    attempts shouldEqual 3
  }

  "retry" should "fail when an exception is not retryable" in {
    val failingFunction: () => Int = () => throw new IllegalArgumentException("Test failure")

    assertThrows[IllegalArgumentException] {
      Retry.retry(customStrategy)(failingFunction())
    }
  }

  "retryAsync" should "succeed after 2 retries" in {
    var attempts = 0
    val failingAsyncFunction: () => Future[Int] = () =>
      Future {
        attempts += 1
        if (attempts < 3) throw new RuntimeException("Test failure") else 42
      }

    val futureResult = Retry.retryAsync(customStrategy)(failingAsyncFunction())
    val result       = Await.result(futureResult, 5.seconds)
    result shouldEqual 42
    attempts shouldEqual 3
  }

  "retryAsync" should "fail when an exception is not retryable" in {
    val failingAsyncFunction: () => Future[Int] = () =>
      Future {
        throw new IllegalArgumentException("Test failure")
      }

    val futureResult = Retry.retryAsync(customStrategy)(failingAsyncFunction())
    assertThrows[IllegalArgumentException] {
      Await.result(futureResult, 5.seconds)
    }
  }

  "Retry" should "retry the operation with the provided delay for all exceptions for a specified number of times" in {
    var i = 0
    val fn = () => {
      i += 1
      if (i == 3) i else throw new RuntimeException("error")
    }
    val result = Retry.retry(RetryStrategy.fixedDelay(2.seconds, 2)) {
      fn()
    }
    result shouldEqual 3
    i shouldEqual 3
  }

  it should "not retry the operation for non-retryable exceptions" in {
    val fn = () => throw new IllegalArgumentException("error")
    assertThrows[IllegalArgumentException] {
      Retry.retry(RetryStrategy.fixedDelay(2.seconds, 2)) {
        fn()
      }
    }

  }

  it should "retry the operation with exponentialBackoff delay for a specified number of times" in {
    var i = 0
    val fn = () => {
      i += 1
      if (i == 3) i else throw new RuntimeException("error")
    }
    val result = Retry.retry(RetryStrategy.exponentialBackoff(2.seconds, 2)) {
      fn()
    }
    result shouldEqual 3
    i shouldEqual 3
  }

  it should "retry the operation with random delay for a specified number of times" in {
    var i = 0
    val fn = () => {
      i += 1
      if (i == 3) i else throw new RuntimeException("error")
    }
    val result = Retry.retry(RetryStrategy.randomDelay(2.seconds, 5.seconds, 2)) {
      fn()
    }
    result shouldEqual 3
    i shouldEqual 3
  }

  it should "retry the operation asynchronously" in {
    var i = 0
    val fn = () =>
      Future {
        i += 1
        if (i == 3) i else throw new RuntimeException("error")
      }
    val result = Await.result(
        Retry.retryAsync(RetryStrategy.fixedDelay(2.seconds, 2)) {
          fn()
        },
        10.seconds
    )
    result shouldEqual 3
    i shouldEqual 3
  }

  // ---- max-retries exhaustion ----

  it should "throw after all retries are exhausted" in {
    var attempts = 0
    val strategy = RetryStrategy.fixedDelay(0.millis, 2)
    assertThrows[RuntimeException] {
      Retry.retry(strategy) {
        attempts += 1
        throw new RuntimeException("always fails")
      }
    }
    attempts shouldEqual 3 // 1 initial + 2 retries
  }

  it should "not retry when maxRetries is 0" in {
    var attempts = 0
    val strategy = RetryStrategy.fixedDelay(0.millis, 0)
    assertThrows[RuntimeException] {
      Retry.retry(strategy) {
        attempts += 1
        throw new RuntimeException("always fails")
      }
    }
    attempts shouldEqual 1
  }

  it should "stop retrying for custom non-retryable exception type" in {
    var attempts = 0
    val strategy = RetryStrategy(
        shouldRetry = (retryCount, _) => if (retryCount < 3) Some(0.millis) else None,
        nonRetryableExceptions = Set(classOf[IllegalStateException])
    )
    assertThrows[IllegalStateException] {
      Retry.retry(strategy) {
        attempts += 1
        throw new IllegalStateException("not retryable")
      }
    }
    attempts shouldEqual 1
  }

  // ---- isRetryable ----

  "RetryStrategy.isRetryable" should "return false for configured non-retryable exceptions" in {
    val strategy = RetryStrategy(
        shouldRetry = (_, _) => Some(0.millis),
        nonRetryableExceptions =
          Set(classOf[IllegalArgumentException], classOf[InterruptedException])
    )
    strategy.isRetryable(new IllegalArgumentException("test")) shouldBe false
    strategy.isRetryable(new InterruptedException("test")) shouldBe false
  }

  it should "return true for exceptions not in the non-retryable set" in {
    val strategy = RetryStrategy(
        shouldRetry = (_, _) => Some(0.millis),
        nonRetryableExceptions = Set(classOf[IllegalArgumentException])
    )
    strategy.isRetryable(new RuntimeException("test")) shouldBe true
    strategy.isRetryable(new Exception("test")) shouldBe true
  }

  it should "return false for a subclass of a configured non-retryable exception" in {
    val strategy = RetryStrategy(
        shouldRetry = (_, _) => Some(0.millis),
        nonRetryableExceptions = Set(classOf[RuntimeException])
    )
    // IllegalArgumentException extends RuntimeException
    strategy.isRetryable(new IllegalArgumentException("subclass")) shouldBe false
  }

  // ---- delay correctness ----

  "RetryStrategy.exponentialBackoff" should "produce correctly doubled delays" in {
    val strategy = RetryStrategy.exponentialBackoff(1.second, 5)
    strategy.shouldRetry(0, new RuntimeException()).map(_.toMillis) shouldEqual Some(1000L)
    strategy.shouldRetry(1, new RuntimeException()).map(_.toMillis) shouldEqual Some(2000L)
    strategy.shouldRetry(2, new RuntimeException()).map(_.toMillis) shouldEqual Some(4000L)
    strategy.shouldRetry(3, new RuntimeException()).map(_.toMillis) shouldEqual Some(8000L)
  }

  it should "return None once maxRetries is reached" in {
    val strategy = RetryStrategy.exponentialBackoff(1.second, 2)
    strategy.shouldRetry(2, new RuntimeException()) shouldEqual None
  }

  "RetryStrategy.fixedDelay" should "always return the same delay within maxRetries" in {
    val strategy = RetryStrategy.fixedDelay(500.millis, 3)
    (0 until 3).foreach { i =>
      strategy.shouldRetry(i, new RuntimeException()) shouldEqual Some(500.millis)
    }
    strategy.shouldRetry(3, new RuntimeException()) shouldEqual None
  }

  "RetryStrategy.randomDelay" should "return delays within [minDelay, maxDelay)" in {
    val strategy = RetryStrategy.randomDelay(100.millis, 500.millis, 20)
    (0 until 20).foreach { i =>
      strategy.shouldRetry(i, new RuntimeException()) match {
        case Some(delay) =>
          delay.toMillis should (be >= 100L and be < 500L)
        case None => fail(s"Expected Some delay for retryCount=$i")
      }
    }
  }

  // ---- retryAsync additional coverage ----

  "retryAsync" should "fail after all retries are exhausted" in {
    var attempts = 0
    val strategy = RetryStrategy.fixedDelay(0.millis, 2)
    val future = Retry.retryAsync(strategy)(Future {
      attempts += 1
      throw new RuntimeException("always fails")
    })
    assertThrows[RuntimeException] {
      Await.result(future, 5.seconds)
    }
    attempts shouldEqual 3 // 1 initial + 2 retries
  }

  it should "not retry non-retryable exceptions" in {
    var attempts = 0
    val strategy = RetryStrategy.fixedDelay(0.millis, 3)
    val future = Retry.retryAsync(strategy)(Future {
      attempts += 1
      throw new IllegalArgumentException("not retryable")
    })
    assertThrows[IllegalArgumentException] {
      Await.result(future, 5.seconds)
    }
    attempts shouldEqual 1
  }
}
