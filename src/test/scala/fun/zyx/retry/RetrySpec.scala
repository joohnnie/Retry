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
}
