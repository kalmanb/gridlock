package kalmanb.lockmanager

import akka.actor.Props
import akka.actor.Actor
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class LockableTest extends AkkaSpec {
  import LockManager._

  describe("lockable") {
    it("should request a lock from the manager") {
      val lockManager = TestProbe()
      val lockingWorker = TestActorRef[LockableActor](Props(new LockableActor(lockManager.ref) {}))
      lockingWorker ! "Start"

      lockManager.expectMsg(RequestLock(lockingWorker.underlyingActor.lockId("")))
    }

    it("should do 'work' if it gets a lock") {
      val lockManager = system.actorOf(Props(new Actor {
        def receive = { case RequestLock(_, _) ⇒ sender ! LockAquired }
      }))

      val latch = new CountDownLatch(1)
      val lockingWorker = TestActorRef[LockableActor](Props(new LockableActor(lockManager) {
        override def work(message: Any, id: Any) {
          latch.countDown
        }
      }))
      lockingWorker ! "Start"

      latch.await(2000, TimeUnit.MILLISECONDS)
      latch.getCount should be(0)
    }

    it("should trigger noLock if it can't aquire a lock") {
      val lockManager = system.actorOf(Props(new Actor {
        def receive = { case _ ⇒ sender ! NoLockAvailable }
      }))
      
      val latch = new CountDownLatch(1)
      val lockingWorker = TestActorRef[LockableActor](Props(new LockableActor(lockManager) {
        override def noLock(message: Any, id: Any) {
          latch.countDown
        }
      }))
      lockingWorker ! "Start"

      latch.await(2000, TimeUnit.MILLISECONDS)
      latch.getCount should be(0)
    }

    it("should cancel lock request if it doesn't the request times out") {
      val latch = new CountDownLatch(1)
      val slowLockManager = TestActorRef(Props(new Actor {
        def receive = {
          case RequestLock(_, _) ⇒ // don't respond to worker - let it timeout
          case ReleaseLock(_)    ⇒ latch.countDown
        }
      }))

      val lockingWorker = TestActorRef[LockableActor](Props(new LockableActor(slowLockManager, 10 milliseconds) {}))
      lockingWorker ! "Start"

      latch.await(2000, TimeUnit.MILLISECONDS)
      latch.getCount should be(0)
    }
  }
}