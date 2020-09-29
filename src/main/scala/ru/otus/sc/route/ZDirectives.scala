package ru.otus.sc.route

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging._
import zio.{CanFail, Has, URIO, URLayer, ZIO}

object ZDirectives {
  type ZDirectives = Has[Service]

  trait Service {
    def completeZio[R: ToResponseMarshaller](zio: URIO[Blocking with Logging with Clock, R]): Route
    def onSuccessZio[R](zio: URIO[Blocking with Logging with Clock, R])(f: R => Route): Route
    def onZio[E: CanFail, R](zio: ZIO[Blocking with Logging with Clock, E, R])(
        f: Either[E, R] => Route
    ): Route
  }

  val live: URLayer[Blocking with Logging with Clock, ZDirectives] =
    ZIO
      .runtime[Blocking with Logging with Clock]
      .map { runtime =>
        new Service {
          def completeZio[R: ToResponseMarshaller](
              zio: URIO[Blocking with Logging with Clock, R]
          ): Route =
            onSuccess(runtime.unsafeRunToFuture(zio)) { res =>
              complete(res)
            }

          def onZio[E: CanFail, R](
              zio: ZIO[Blocking with Logging with Clock, E, R]
          )(f: Either[E, R] => Route): Route =
            onSuccess(runtime.unsafeRunToFuture(zio.either))(f)

          def onSuccessZio[R](
              zio: URIO[Blocking with Logging with Clock, R]
          )(f: R => Route): Route = onSuccess(runtime.unsafeRunToFuture(zio))(f)
        }
      }
      .toLayer

}
