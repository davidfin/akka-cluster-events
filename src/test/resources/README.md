cluster-events

## Exercise 3 > Cluster Events

In this exercise we **dynamically bind** to remote `PlayerRegistry` instances by implementing Akka Cluster Events. In order to do this, we will focus on changing the following:

- `application.conf`:
    - Implement the `ClusterActorRefProvider`.
    - **ATTENTION**: For `hostname` use `localhost`.
    - **HINT**: You may need to remove the `player-registry` section.

- `package.scala` (add the following code):

```scala
implicit class VectorOps[A](as: Vector[A]) {
  def -(a: A): Vector[A] =
    as diff Vector(a)
}

val VNil: Vector[Nothing] =
  Vector.empty
```

- `GameEngine.scala` (implement new transitions):
    - Add a `Waiting` state:
        - Transition form `Waiting` to `Pausing` when a member node with `player-registry` role becomes available.
        - **HINT**: Use `Event(MemberUp(member), ...)`
    - In the `Pausing` state:
        - Handle `MemberUp`.
        - Handle `MemberRemoved`.
        - Handle `StateTimeout`.
    - In the `Running` state:
        - Handle `MemberUp`.
        - Handle `MemberRemoved`.
        - Handle `Terminated`.
    - **HINT**: You will need to implement `preStart` to handle cluster subscription.
    - **HINT**: You will need to implement `postStop` to unsubscribe from the cluster.
    - **HINT**: `createTournament` will need the `playerRegistry`.
    - **HINT**: You should implement the `selectPlayerRegistry` and `isPlayerRegistry` methods.
    - **HINT**: You want to make sure the proper roles are joining your cluster!
- `GameEngineApp.scala`:
    - **HINT**: `playerRegistryAddress` is no longer needed.
- `Settings.scala`:
    - **HINT**: `playerRegistryAddress` is no longer needed.

Now we have our taken our next step in making Akka Collect **reactive**. It is time to test our solution.

- Use the `ge` command alias to bootstrap `GameEngine`.
- Open an new terminal window and start another `sbt` session.
- Use the `pr` command alias to bootstrap the `PlayerRegistry`.
- From the `PlayerRegistry` prompt, create two `simple` players.
- Use the `pr2` command alias to bootstrap a second `PlayerRegistry`.
- From the second `PlayerRegistry` prompt, create two `simple` players.
- Open up a new terminal session and `tail -f akka-collect.log` and verify the Akka Collect is working as expected.
- Shutdown one of the `PlayerRegistry` instances and notice what the `akka-collect.log` reports.
- Use the `test` command to verify everything works as expected.
- Use the `koan next` command to move to the next exercise.
