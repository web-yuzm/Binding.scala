/*
The MIT License (MIT)

Copyright (c) 2016 Yang Bo & REA Group Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.thoughtworks.binding

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.language.higherKinds
import scala.util.Try
import scala.language.experimental.macros
import Binding._

object FutureBinding {
  def apply[A](future: Future[A])(implicit executor: ExecutionContext) = new FutureBinding(future)
}

/**
  * A wrapper that wraps [[scala.concurrent.Future]] to a [[Binding]].
  *
  * @note Because all [[Binding]] (including this [[FutureBinding]]) are not thread safe,
  *       you must guarantee `executor` running sequencely.
  */
final class FutureBinding[A](future: Future[A])(implicit executor: ExecutionContext) extends Binding[Option[Try[A]]] {

  override def get = future.value

  private val publisher = new Publisher[ChangedListener[Option[Try[A]]]]

  override private[binding] def removeChangedListener(listener: ChangedListener[Option[Try[A]]]): Unit = {
    publisher.unsubscribe(listener)
  }

  private var isHandlerRegiested: Boolean = false

  private def completeHandler(result: Try[A]): Unit = {
    for (listener <- publisher) {
      listener.changed(new ChangedEvent[Option[Try[A]]](this, None, Some(result)))
    }
  }

  override private[binding] def addChangedListener(listener: ChangedListener[Option[Try[A]]]): Unit = {
    if (!isHandlerRegiested) {
      isHandlerRegiested = true
      if (!future.isCompleted) {
        future.onComplete(completeHandler)
      }
    }
    publisher.subscribe(listener)

  }

}
