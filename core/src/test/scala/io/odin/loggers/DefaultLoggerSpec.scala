package io.odin.loggers

import cats.data.Writer
import cats.effect.Clock
import cats.instances.list._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

import scala.concurrent.duration.TimeUnit

class DefaultLoggerSpec extends OdinSpec {

  type F[A] = Writer[List[LoggerMessage], A]

  it should "correctly construct LoggerMessage" in {
    forAll { (msg: String, ctx: Map[String, String], throwable: Throwable, timestamp: Long) =>
      implicit val clk: Clock[F] = clock(timestamp)
      val log = logger
      check(log.trace(msg))(Level.Trace, msg, timestamp)
      check(log.trace(msg, throwable))(Level.Trace, msg, timestamp, throwable = Some(throwable))
      check(log.trace(msg, ctx))(Level.Trace, msg, timestamp, ctx)
      check(log.trace(msg, ctx, throwable))(Level.Trace, msg, timestamp, ctx, Some(throwable))

      check(log.debug(msg))(Level.Debug, msg, timestamp)
      check(log.debug(msg, throwable))(Level.Debug, msg, timestamp, throwable = Some(throwable))
      check(log.debug(msg, ctx))(Level.Debug, msg, timestamp, ctx)
      check(log.debug(msg, ctx, throwable))(Level.Debug, msg, timestamp, ctx, Some(throwable))

      check(log.info(msg))(Level.Info, msg, timestamp)
      check(log.info(msg, throwable))(Level.Info, msg, timestamp, throwable = Some(throwable))
      check(log.info(msg, ctx))(Level.Info, msg, timestamp, ctx)
      check(log.info(msg, ctx, throwable))(Level.Info, msg, timestamp, ctx, Some(throwable))

      check(log.warn(msg))(Level.Warn, msg, timestamp)
      check(log.warn(msg, throwable))(Level.Warn, msg, timestamp, throwable = Some(throwable))
      check(log.warn(msg, ctx))(Level.Warn, msg, timestamp, ctx)
      check(log.warn(msg, ctx, throwable))(Level.Warn, msg, timestamp, ctx, Some(throwable))

      check(log.error(msg))(Level.Error, msg, timestamp)
      check(log.error(msg, throwable))(Level.Error, msg, timestamp, throwable = Some(throwable))
      check(log.error(msg, ctx))(Level.Error, msg, timestamp, ctx)
      check(log.error(msg, ctx, throwable))(Level.Error, msg, timestamp, ctx, Some(throwable))
    }
  }

  private def clock(timestamp: Long): Clock[F] = new Clock[F] {
    def realTime(unit: TimeUnit): F[Long] = Writer.value(timestamp)
    def monotonic(unit: TimeUnit): F[Long] = Writer.value(timestamp)
  }

  private def logger(implicit clock: Clock[F]): Logger[F] = new DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = Writer.tell(List(msg))
  }

  private def check(
      fn: => F[Unit]
  )(
      level: Level,
      msg: String,
      timestamp: Long,
      ctx: Map[String, String] = Map.empty,
      throwable: Option[Throwable] = None
  ) = {
    val List(loggerMessage) = fn.written
    loggerMessage.level shouldBe level
    loggerMessage.message() shouldBe msg
    loggerMessage.context shouldBe ctx
    loggerMessage.exception shouldBe throwable
    loggerMessage.threadName shouldBe Thread.currentThread().getName
    loggerMessage.timestamp shouldBe timestamp
  }

}