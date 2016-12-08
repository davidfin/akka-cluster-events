/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.ask
import scala.annotation.tailrec
import scala.io.StdIn

object PlayerRegistryApp extends BaseApp with Terminal {

  override protected val parser: CommandParser.Parser[Command] =
    CommandParser.register | CommandParser.shutdown

  override def createTop(system: ActorSystem, settings: Settings): ActorRef =
    system.actorOf(PlayerRegistry.props, PlayerRegistry.name)

  @tailrec
  override protected def commandLoop(system: ActorSystem, settings: Settings, top: ActorRef): Unit = {
    def register(name: String, props: Props, count: Int): Unit = {
      def askRegister(name: String, props: Props) = {
        import settings.app.askTimeout
        val registerPlayerResponse = top ? PlayerRegistry.RegisterPlayer(name, props)
        import scala.concurrent.ExecutionContext.Implicits.global
        registerPlayerResponse onSuccess {
          case PlayerRegistry.PlayerNameTaken(name)  => system.log.warning("Player name {} taken", name)
          case PlayerRegistry.PlayerRegistered(name) => system.log.warning("Registered player {}", name)
        }
        registerPlayerResponse onFailure {
          case _ => system.log.error("No response from player registry for registering player {}!", name)
        }
      }
      if (count != 1)
        for (n <- 1 to count)
          askRegister(s"$name-$n", props)
      else
        askRegister(name, props)
    }
    Command(StdIn.readLine()) match {
      case Command.Register(name, props, count) =>
        register(name, props, count)
        commandLoop(system, settings, top)
      case Command.Shutdown =>
        system.terminate()
      case Command.Unknown(command, message) =>
        system.log.warning("Unknown command {} ({})!", command, message)
        commandLoop(system, settings, top)
    }
  }
}
