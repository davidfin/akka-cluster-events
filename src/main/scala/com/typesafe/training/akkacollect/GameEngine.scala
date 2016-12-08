/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Address, FSM, Props, Terminated}
import akka.cluster.{Cluster, Member}

import scala.concurrent.duration.FiniteDuration

object GameEngine {

  sealed trait State

  object State {

    case object Pausing extends State

    case object Running extends State

    case object Waiting extends State
  }

  case class Data(tournament: Option[ActorRef] = None)

  val name: String =
    "game-engine"

  def props(tournamentInterval: FiniteDuration, scoresRepository: ActorRef): Props =
    Props(new GameEngine(tournamentInterval, scoresRepository))
}

class GameEngine(tournamentInterval: FiniteDuration, scoresRepository: ActorRef)
  extends Actor with FSM[GameEngine.State, GameEngine.Data] with SettingsActor with ActorLogging {

  import GameEngine._
  import akka.cluster.ClusterEvent._

  override def preStart(): Unit = {
    // need initialStateAsEvents: ask to send the state of the cluster as soon as we go up.
    // don't have a dependency of game-engine and playerregistry order.
    // MemberEvent is trait that contains MemberUp, MemberRemoved
    Cluster(context.system).subscribe(self, InitialStateAsEvents, classOf[MemberEvent])
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
  }

  startWith(State.Waiting, Data() )

  when(State.Waiting){
    case Event(MemberUp(member), data @ Data(None)) if isPlayerRegistry(member) =>
       goto(State.Pausing) using data
  }

  when(State.Running) {
    case Event(MemberUp(member), data @ Data(None)) if isPlayerRegistry(member) =>
      stay using data
    case Event(MemberRemoved(member,_), data @ Data(Some(_))) =>
      stay using data
    case Event(Terminated(_), data) =>
      goto(State.Pausing) using data.copy(tournament = None)
  }

  when(State.Pausing){
    case Event(MemberUp(member), data @ Data(None)) if isPlayerRegistry(member) =>
      val playerReg = selectPlayerRegistry
      playerRegistryTransition(playerReg, data)

    case Event(MemberRemoved(member,_), data @ Data(None)) =>
      goto(State.Running) using data

    case Event(StateTimeout, data @ Data(None)) =>
      val playerReg = selectPlayerRegistry
      playerRegistryTransition(playerReg, data)
  }

  onTransition {
    case _ -> State.Pausing => log.debug("Transitioning into pausing state")
    case _ -> State.Running => log.debug("Transitioning into running state")
  }

  initialize()

  def startTournament(playerRegistry:ActorSelection): ActorRef = {
    log.info("Starting tournament")
    context.watch(createTournament(playerRegistry))
  }

  def createTournament(playerRegistry:ActorSelection): ActorRef = {
    import settings.tournament._
    context.actorOf(Tournament.props(playerRegistry, scoresRepository, maxPlayerCountPerGame, askTimeout))
  }

  // check if member has the player-registry role
  private def isPlayerRegistry(member: Member): Boolean =
    member hasRole PlayerRegistry.name

  private def selectPlayerRegistry: Option[ActorSelection] = {
    val member: Option[Member] = Cluster(context.system).state.members.find(_.hasRole("player-registry"))
    member.map(m => context actorSelection PlayerRegistry.pathFor(m.address))
  }

  private def playerRegistryTransition(playerReg: Option[ActorSelection], data:Data):
  FSM.State[GameEngine.State, GameEngine.Data] ={
    playerReg match {
      case Some(pr) =>
        goto(State.Running) using data.copy(tournament = Some(startTournament(pr)))
      case None =>
        goto(State.Waiting) using Data(None)
    }
  }

}
