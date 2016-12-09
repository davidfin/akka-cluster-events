# akka-training-cluster-events

This project was developed as part of the advanced akka training course by LightBend. 

## Akka Cluster
Akka Cluster provides a fault-tolerant decentralized peer-to-peer based cluster membership service with no single point of failure or single point of bottleneck. Gossip protocols are used along with an automatic failure detector.

A cluster is made up from a set of actor systems forming member nodes. Member nodes can join or leave a cluster any time. Joining a cluster automatically is facilitated by seed nodes. Seed nodes are initial contact points for joining nodes and are used by actor systems for auto-joining the cluster. When a node becomes unavailable, it can be auto-downed using the auto-down-unreachable-after configuration. Note that this can be very dangerous because it can lead to forming separate clusters (also known as the split-brain problem).Member nodes may perform different functions, the role is assigned using the akka.cluster.roles configuration.

```config
actor {
    provider = akka.cluster.ClusterActorRefProvider 
    cluster {
    metrics.enabled=off
    auto-down-unreachable-after = 5 seconds // shut down and considered left the cluster.
    seed-nodes                  = [
      "akka.tcp://akkollect-system@localhost:2551",
      "akka.tcp://akkollect-system@localhost:2552"
    ]
  }
}
```

## Akka Cluster Events

An actor can subscribe to cluster change notifications.

Some interesting events are

1. MemberUp: member status changed to Up.
2. UnreachableMember: member considered unreachable by failure detector.
3. MemberRemoved: member removed from the cluster.
4. CurrentMemberState: current snapshot state of the cluster unless InitialStateAsEvents specified. 

## Exercise

In this exercise we dynamically bind to remote PlayerRegistry instances by implementing Akka Cluster Events. The focus is on changing the file GameEngine.scala 

1. Add a Waiting state to Transition form Waiting to Pausing when a member node with player-registry role becomes available.
2. In the Pausing state handle MemberUp, MemberRemoved, and StateTimeout Events. 
3. In The Running state handle MemberUp, MemberRemoved, and Terminated.
4. preStart to handle cluster subscription 
5. postStop to unsubscribe from the cluster
6. Implement isPlayerRegistry to check if the Cluster Member has the role "player-registry" 
7. Implement selectPlayerRegistry to select a Cluster Member has the role "player-registry"

### command aliases (ge, pr, sr, ge2, pr2, sr2 and sj)
```scala
ge  // runs the game engine on port 2551
pr  // runs the player registry on port 2552
sr  // runs the scores repository on port 2553
ge2 // runs the game engine on port 2554
pr2 // runs the player registry on port 2555
sr2 // runs the scores repository on port 2556
sj  // runs the shared journal on port 2550
```

