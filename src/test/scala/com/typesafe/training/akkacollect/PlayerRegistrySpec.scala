package com.typesafe.training.akkacollect

import akka.actor.Address
import akka.testkit.ImplicitSender

class PlayerRegistrySpec extends BaseAkkaSpec with ImplicitSender {

  import PlayerRegistry._

  "The player registry actor" should {

    implicit val timeout = testKitSettings.DefaultTimeout

    "allow registration of users" in {
      val registry = system.actorOf(PlayerRegistry.props)
      registry ! RegisterPlayer("jill", SimplePlayer.props)
      expectMsg(PlayerRegistered("jill"))
    }

    "return PlayerIsAlreadyTaken if the name is already taken" in {
      val registry = system.actorOf(PlayerRegistry.props)
      registry ! RegisterPlayer("jill", SimplePlayer.props)
      expectMsg(PlayerRegistered("jill"))
      registry ! RegisterPlayer("jill", SimplePlayer.props)
      expectMsg(PlayerNameTaken("jill"))
    }

  }

  "The player registry companion" should {

    "create an an actor path given an address" in {
      val address = Address("akka.tcp", "system", "example.com", 1899)
      val path = PlayerRegistry.pathFor(address)
      path.address shouldEqual address
      path.name shouldEqual PlayerRegistry.name
      path.parent.name shouldEqual "user"
    }

  }



}
