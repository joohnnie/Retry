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

import java.util.concurrent.{Executors, TimeUnit}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * The Retry object provides utilities for retrying functions that may throw exceptions.
 */
object Retry {

  private val scheduler = Executors.newSingleThreadScheduledExecutor { r =>
    val t = new Thread(r, "retry-scheduler")
    t.setDaemon(true)
    t
  }

  /**
   * Retries a given function synchronously based on the provided RetryStrategy.
   *
   * @param strategy
   *   The retry strategy to use for handling retries and exceptions.
   * @param fn
   *   The function to execute and retry in case of a retryable exception.
   * @tparam T
   *   The return type of the function.
   * @return
   *   The result of the function or throws an exception if all retries fail.
   */
  def retry[T](strategy: RetryStrategy)(fn: => T): T = {
    @tailrec
    def retryHelper(retryCount: Int): Try[T] = {
      val result = Try(fn)
      result match {
        case Failure(exception) if strategy.isRetryable(exception) =>
          strategy.shouldRetry(retryCount, exception) match {
            case Some(delay) =>
              Thread.sleep(delay.toMillis)
              retryHelper(retryCount + 1)
            case None => result
          }
        case _ => result
      }
    }

    retryHelper(0) match {
      case Failure(exception) => throw exception
      case Success(value)     => value
    }
  }

  /**
   * Retries a given function asynchronously based on the provided RetryStrategy.
   *
   * @param strategy
   *   The retry strategy to use for handling retries and exceptions.
   * @param fn
   *   The function to execute and retry in case of a retryable exception.
   * @param ec
   *   The ExecutionContext to use for scheduling retries.
   * @tparam T
   *   The return type of the function.
   * @return
   *   A Future containing the result of the function or a failed Future if all retries fail.
   */
  def retryAsync[T](
      strategy: RetryStrategy
  )(fn: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]()

    def retryHelper(retryCount: Int): Unit = {
      fn.onComplete {
        case Success(value) => promise.success(value)
        case Failure(exception) if strategy.isRetryable(exception) =>
          strategy.shouldRetry(retryCount, exception) match {
            case Some(delay) =>
              scheduler.schedule(
                (() => retryHelper(retryCount + 1)): Runnable,
                delay.toMillis,
                TimeUnit.MILLISECONDS
              )
            case None => promise.failure(exception)
          }
        case Failure(exception) => promise.failure(exception)
      }
    }

    retryHelper(0)
    promise.future
  }
}
