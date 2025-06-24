package org.zaquest.tgchat.store

import cats.{Functor}
import cats.effect.Concurrent
import cats.effect.std.MapRef
import cats.implicits._

import org.zaquest.tgchat.chat.{Session, given}


type ChatId = Long


trait SessionStore[F[_]]:
  def getOrCreate(id: ChatId, default: Session): F[Session]
  def set(id: ChatId, session: Session): F[Unit]
  def delete(id: ChatId): F[Unit]


object SessionStore:

  def inMemory[F[_]: Concurrent]: F[SessionStore[F]] =
    // Arbitrary number of shards
    MapRef.ofShardedImmutableMap[F, ChatId, Session](shardCount = 5)
      .map { mapRef =>
        new SessionStore[F] {
          override def getOrCreate(
            id: ChatId,
            default: Session,
          ): F[Session] =
            val mapRefDefault = mapRef.withDefaultValue(default)
            mapRefDefault(id).get

          override def set(id: ChatId, session: Session): F[Unit] =
            mapRef.setKeyValue(id, session)

          override def delete(id: ChatId): F[Unit] =
            mapRef.unsetKey(id)
        }
      }
