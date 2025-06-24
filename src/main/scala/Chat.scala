package org.zaquest.tgchat.chat

import cats.ApplicativeError
import cats.kernel.Eq
import cats.implicits._
import cats.effect.Concurrent

import io.circe.{Codec, Encoder, Decoder, HCursor, Json}
import io.circe.derivation.{Configuration, ConfiguredEnumCodec}
import io.circe.DecodingFailure
import io.circe.DecodingFailure.Reason.CustomReason
import io.circe.syntax._
import scala.deriving.Mirror

import org.typelevel.ci._
import org.http4s.{Uri, Request, Method, ParseFailure}
import org.http4s.client.Client
import org.http4s.{Headers, Header}
import org.http4s.client.dsl.io._
import org.http4s.circe._

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert


implicit val uriConfigReader: ConfigReader[Uri] =
  ConfigReader.fromCursor { cursor =>
    cursor.asString.flatMap(url =>
      Uri.fromString(url) match
        case Right(uri) => Right(uri)
        case Left(ParseFailure(sanitized, details)) =>
          val because = s"${sanitized} ${details}"
          val reason = CannotConvert(url, "Uri", because)
          cursor.failed(reason)
    )
  }


case class ChatConfig (
  url: Uri,
  model: String,
  apiKey: String,
  system: String,
)


enum ChatRole:
  case System, Assistant, User


object ChatRole:

  given ConfiguredEnumCodec[ChatRole] =
    ConfiguredEnumCodec.derived[ChatRole](
      using
      summon[Mirror.SumOf[ChatRole]],
      Configuration.default
        .withTransformConstructorNames(_.toLowerCase),
    )


case class ChatMessage(
  role: ChatRole,
  content: String,
) derives Codec


given Eq[ChatMessage] = Eq.fromUniversalEquals


type Session = Vector[ChatMessage]


case class ChatRequest(
  model: String,
  messages: Vector[ChatMessage],
  stream: Boolean,
) derives Codec


case class ChatResponse(
  message: ChatMessage,
) derives Codec


trait ChatClient[F[_]]:
  def newSession: Session
  def ask(session: Session, prompt: String): F[Session]


class OpenAiChatClient[F[_]: Concurrent](
  conf: ChatConfig,
  http: Client[F],
)(using ae: ApplicativeError[F, Throwable]) extends ChatClient[F]:

  def newSession: Session =
    Vector(ChatMessage(ChatRole.System, conf.system))

  def ask(session: Session, prompt: String): F[Session] =
    val updatedSession = session :+ ChatMessage(ChatRole.User, prompt)
    val authHeader =
      Header.Raw(ci"Authorization", s"Bearer ${conf.apiKey}")
    val req = Request[F](Method.POST, uri = conf.url)
      .withHeaders(authHeader)
      .withEntity(ChatRequest(
        model = conf.model,
        messages = updatedSession,
        stream = false,  // streaming is not supported
      ).asJson)
    http.expect(req)(using jsonOf[F, ChatResponse])
      .map(updatedSession :+ _.message)
      .handleErrorWith({ err =>
        err.printStackTrace()
        val errMsg = ChatMessage(
          ChatRole.System, "Произошла ошибка, попробуйте еще раз.")
        ae.pure(session :+ errMsg)
      })
