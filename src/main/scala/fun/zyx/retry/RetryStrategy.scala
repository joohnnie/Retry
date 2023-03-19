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

import fun.zyx.retry.RetryStrategy.defaultNonRetryableExceptions

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Random

/**
 * A case class that defines the retry strategy for retryable exceptions.
 *
 * @param shouldRetry
 *   a function that returns the time duration to wait before the next retry. If this function
 *   returns None, the retry attempts will be stopped.
 * @param nonRetryableExceptions
 *   a set of exceptions that should not be retried. Any exception that is an instance of one of
 *   these exceptions will be marked as non-retryable and the retry attempts will be stopped.
 */
case class RetryStrategy(
    shouldRetry: (Int, Throwable) => Option[Duration],
    nonRetryableExceptions: Set[Class[_ <: Throwable]] = defaultNonRetryableExceptions
) {

  /**
   * A method that checks if the exception is retryable or not based on the nonRetryableExceptions
   * set.
   *
   * @param exception
   *   the exception to check for retryability
   * @return
   *   true if the exception is retryable, false otherwise
   */
  def isRetryable(exception: Throwable): Boolean = {
    !nonRetryableExceptions.exists(_.isAssignableFrom(exception.getClass))
  }
}

object RetryStrategy {
  // Define some default NonRetryableExceptions
  val defaultNonRetryableExceptions: Set[Class[_ <: Throwable]] =
    Set(classOf[InterruptedException], classOf[IllegalArgumentException])

  private val DEFAULT_MAX_RETRIES: Int = 3

  def fixedDelay(delay: Duration, maxRetries: Int = DEFAULT_MAX_RETRIES): RetryStrategy = {
    RetryStrategy(
        shouldRetry = (retryCount: Int, exception: Throwable) =>
          if (retryCount <= maxRetries) Some(delay) else None
    )
  }

  def exponentialBackoff(
      initialDelay: Duration,
      maxRetries: Int = DEFAULT_MAX_RETRIES
  ): RetryStrategy = {
    RetryStrategy(
        shouldRetry = (retryCount: Int, exception: Throwable) =>
          if (retryCount <= maxRetries) Some(initialDelay * (2 ^ retryCount)) else None
    )
  }

  def randomDelay(
      minDelay: Duration,
      maxDelay: Duration,
      maxRetries: Int = DEFAULT_MAX_RETRIES
  ): RetryStrategy = {

    RetryStrategy(
        shouldRetry = (retryCount: Int, exception: Throwable) =>
          if (retryCount <= maxRetries) {
            val delay =
              minDelay.toMillis + Random.nextInt((maxDelay.toMillis - minDelay.toMillis).toInt)
            Some(delay.millis)
          } else None
    )
  }
}
