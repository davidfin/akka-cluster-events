package com.typesafe.training.akkacollect

import akka.actor._
import akka.testkit.{TestFSMRef, TestProbe}

import scala.concurrent.duration._

class GameEngineSpec extends BaseAkkaSpec {

  "The game engine actor" should {

    "transition into GameEngine.State.Running with a tournament" in pendingUntilFixed {
      val engine = TestFSMRef(new GameEngine(
        tournamentInterval = 1 millisecond,
        scoresRepository = system.deadLetters
      ))

      awaitCond(engine.stateName == GameEngine.State.Running)
      awaitCond(engine.stateData.tournament.isDefined)

      system.stop(engine)
    }

    "keep starting new tournaments when a tournament ends" in pendingUntilFixed {
      val tournament = TestProbe()

      class TestEngine extends GameEngine(
        tournamentInterval = 1 millisecond,
        scoresRepository = system.deadLetters
      ) {

        // it will toggle back and forth as tournaments ends, so
        // we give it a fake tournament that will end first, and then
        // the deadLetters actor which will not terminate, so it will
        // get stuck in the running state after "creating" two tournaments
        var tournaments = Seq(tournament.ref, system.deadLetters)

        override def createTournament(playerRegistry: ActorSelection): ActorRef = {
          val current +: next = tournaments
          tournaments = next

          current
        }
      }
      val engine = TestFSMRef(new TestEngine)

      awaitCond(engine.stateName == GameEngine.State.Running)
      system.stop(tournament.ref)

      awaitCond(
        engine.underlyingActor.tournaments.isEmpty,
        message = s"new tournament not started upon tournament termination, ${engine.underlyingActor.tournaments}")

    }

  }

}
