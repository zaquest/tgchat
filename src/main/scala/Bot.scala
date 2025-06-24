package org.zaquest.tgchat.bot

import cats.{Applicative, Parallel}
import cats.effect.Async
import cats.implicits._
import cats.syntax.apply._

import org.http4s.client.Client

import telegramium.bots.{Message, ChatIntId, Markdown}
import telegramium.bots.high.{Api, LongPollBot, Methods, BotApi}
import telegramium.bots.high.implicits._

import org.zaquest.tgchat.chat.ChatClient
import org.zaquest.tgchat.store.{SessionStore, ChatId}
import telegramium.bots.BotCommandScopeAllPrivateChats
import telegramium.bots.BotCommand

case class TelegramConfig(
    token: String
)

class TgChatBot[F[_]: Parallel: Async](
    chat: ChatClient[F],
    store: SessionStore[F]
)(using api: Api[F])
    extends LongPollBot[F](api):

  def unit: F[Unit] =
    summon[Applicative[F]].pure(())

  override def onMessage(msg: Message): F[Unit] =
    if msg.chat.`type` != "private" then unit
    else
      val chatId = msg.chat.id
      msg.text match
        case None           => unit
        case Some("/clear") => store.delete(chatId)
        case Some(question) => answer(chatId, question)

  def answer(chatId: ChatId, question: String): F[Unit] =
    for {
      prevSession <- store.getOrCreate(chatId, chat.newSession)
      session <- chat.ask(prevSession, question)
      _ <- store.set(chatId, session)
      _ <- Methods
        .sendMessage(
          chatId = ChatIntId(chatId),
          text = session.last.content
        )
        .exec
    } yield ()

  def setCommands(): F[Unit] =
    setMyCommands(
      commands = List(
        BotCommand("/clear", "Забыть текущий разговор.")
      ),
      scope = Some(BotCommandScopeAllPrivateChats),
      languageCode = Some("ru")
    ).exec.void
    setMyCommands(
      commands = List(
        BotCommand("/clear", "Forget current conversation.")
      ),
      scope = Some(BotCommandScopeAllPrivateChats),
    ).exec.void

object TgChatBot:

  def create[F[_]: Parallel: Async](
      conf: TelegramConfig,
      httpClient: Client[F],
      chat: ChatClient[F],
      store: SessionStore[F]
  ): F[TgChatBot[F]] =
    val baseUrl = s"https://api.telegram.org/bot${conf.token}"
    given api: Api[F] = BotApi[F](httpClient, baseUrl)
    val bot = TgChatBot[F](chat, store)
    bot.setCommands() *> summon[Applicative[F]].pure(bot)
