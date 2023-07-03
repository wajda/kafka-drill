package com.github.wajda.kadrill

import java.util.concurrent.{CompletableFuture, CompletionStage}
import scala.concurrent.{Future, Promise}

object FutureConverters {

  class JavaFutureOps[A](javaFuture: CompletionStage[A]) extends AnyVal {
    def toScala: Future[A] = {
      val promise = Promise[A]()
      javaFuture.whenComplete((result: A, exception: Throwable) => {
        if (exception == null) promise.success(result)
        else promise.failure(exception)
      })
      promise.future
    }
  }
}
