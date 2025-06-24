package org.zaquest.tgchat.main

import cats.effect.IO

import pureconfig.{ConfigReader, ConfigSource}

import org.zaquest.tgchat.chat.{ChatConfig, OpenAiChatClient, given}
import org.zaquest.tgchat.store.SessionStore
import org.zaquest.tgchat.utils.{runIOSimple, withHttpClient}
import org.zaquest.tgchat.bot.{TelegramConfig, TgChatBot}


case class BotConfig(
  telegram: TelegramConfig,
  chat: ChatConfig,
) derives ConfigReader


@main
def lawyerBotMain(): Unit =
  val conf = ConfigSource.default.loadOrThrow[BotConfig]
  runIOSimple {
    withHttpClient { httpClient =>
      val chat = OpenAiChatClient[IO](conf.chat, httpClient)
      for {
        store <- SessionStore.inMemory[IO]
        bot <- TgChatBot.create[IO](
          conf.telegram, httpClient, chat, store)
        _ <- bot.start()
      } yield ()
    }
  }
