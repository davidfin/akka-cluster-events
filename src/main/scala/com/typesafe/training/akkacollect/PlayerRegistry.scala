/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef, Address, Props, RootActorPath }

object PlayerRegistry {

  case class RegisterPlayer(name: String, props: Props)

  case class PlayerNameTaken(name: String)

  case class PlayerRegistered(name: String)

  case object GetPlayers

  case class Players(players: Set[ActorRef])

  val name: String =
    "player-registry"
  
  def pathFor(address: Address): ActorPath =
    RootActorPath(address) / "user" / name

  def props: Props =
    Props(new PlayerRegistry)
}

class PlayerRegistry extends Actor with ActorLogging {

  import PlayerRegistry._

  override def receive: Receive = {
    case RegisterPlayer(name, _) if isNameTaken(name) => playerNameTaken(name: String)
    case RegisterPlayer(name, props)                  => registerPlayer(name, props)
    case GetPlayers                                   => sender() ! Players(context.children.toSet)
  }

  private def playerNameTaken(name: String): Unit = {
    log.warning("Player name {} taken", name)
    sender() ! PlayerNameTaken(name)
  }

  private def registerPlayer(name: String, props: Props): Unit = {
    log.info("Registering player {}", name)
    createPlayer(name, props)
    sender() ! PlayerRegistered(name)
  }

  protected def createPlayer(name: String, props: Props): ActorRef =
    context.actorOf(props, name)

  private def isNameTaken(name: String): Boolean =
    context.child(name).isDefined
}
