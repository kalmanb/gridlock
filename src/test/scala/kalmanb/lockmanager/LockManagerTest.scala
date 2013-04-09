package kalmanb.lockmanager

import scala.concurrent.duration.DurationInt

import LockManager.LockAcquired
import LockManager.NoLockAvailable
import LockManager.ReleaseLock
import LockManager.RequestLock
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.testkit.TestActorRef

class LockManagerTest extends AkkaSpec {
  import LockManager._
  
  describe("lock manager") {
    val anId = "id"
    it("should grant locks") {
      val lockManager = system.actorOf(Props(new LockManager))
      lockManager ! RequestLock(anId)
      expectMsg(LockAcquired)
    }

    it("should only allow a multiple locks at a time") {
      val lockManager = TestActorRef(Props(new LockManager))
      lockManager ! RequestLock("id-1")
      lockManager ! RequestLock("id-2")
      expectMsg(LockAcquired)
      expectMsg(LockAcquired)
    }

    it("should by default it should only allow a single lock at a time") {
      val lockManager = TestActorRef(Props(new LockManager))
      lockManager ! RequestLock(anId)
      expectMsg(LockAcquired)

      lockManager ! RequestLock(anId)
      expectMsg(NoLockAvailable)
    }

    it("should clear locks allowing them to be aquired again") {
      val lockManager = TestActorRef(Props(new LockManager))
      lockManager ! RequestLock(anId)
      expectMsg(LockAcquired)
      lockManager ! RequestLock(anId)
      expectMsg(NoLockAvailable)

      lockManager ! ReleaseLock(anId)
      lockManager ! RequestLock(anId)
      expectMsg(LockAcquired)
    }

    it("should support cancelling a lock that doesn't exist") {
      val lockManager = TestActorRef[LockManager](Props(new LockManager))
      lockManager ! ReleaseLock(anId)
    }

    describe("autoReleaseAfter") {
      it("should timeout locks if autoReleaseAfter is set") {
        val lockManager = TestActorRef(Props(new LockManager))
        lockManager ! RequestLock(anId, autoReleaseAfter = Some(10 millis))
        expectMsg(LockAcquired)
        lockManager ! RequestLock(anId)
        expectMsg(NoLockAvailable)

        Thread sleep 100

        lockManager ! RequestLock(anId)
        expectMsg(LockAcquired)
      }

      it("should not cancel a completed lock") {
        val lockManager = TestActorRef[LockManager](Props(new LockManager))
        lockManager ! RequestLock(anId, autoReleaseAfter = Some(10 minutes))
        expectMsg(LockAcquired)
        lockManager ! RequestLock(anId)
        expectMsg(NoLockAvailable)

        val lockTimeout = lockManager.underlyingActor.locks get (anId.hashCode)
        val cancellable = lockTimeout.get.get

        cancellable.isCancelled should be(false)

        lockManager ! ReleaseLock(anId)
        cancellable.isCancelled should be(true)
      }
    }

//    describe("waitTimeout") {
//      it("should fail to obtain lock after timeout") {
//        val lockManager = TestActorRef(Props(new LockManager))
//        lockManager ! RequestLock(anId)
//        expectMsg(LockAcquired)
//        lockManager ! RequestLock(anId)
//        expectMsg(NoLockAvailable)
//
//        Thread sleep 100
//
//        lockManager ! RequestLock(anId)
//        expectMsg(LockAcquired)
//      }
//
//    }

  }
}
