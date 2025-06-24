package org.zaquest.tgchat.utils

import cats.effect.ExitCode
import cats.effect.{IO, IOApp, Async}

import fs2.io.net.Network
import fs2.compression.Compression
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, GZip}
import org.http4s.ember.client.EmberClientBuilder


def runIO(action: IO[ExitCode]): Unit =
  object App extends IOApp:
    def run(args: List[String]) = action
  App.main(Array[String]())


def runIOSimple[A](action: IO[A]): Unit =
  runIO(action.as(ExitCode.Success))


def withHttpClient[F[_]: Async: Network: Compression, B](
  body: (Client[F]) => F[B]
): F[B] =
  EmberClientBuilder.default[F].build.use { httpClient =>
    var client = httpClient
    client = GZip()(client)
    client = FollowRedirect(maxRedirects = 3)(client)
    body(client)
  }
