package kalmanb.lockmanager

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Cancellable

object LockManager {
  case class RequestLock(id: Any, autoReleaseAfter: Option[FiniteDuration] = None)
  case class ReleaseLock(id: Any)
  case class LockTimeout(id: Any)
  
//  sealed trait LockResult
  case object LockAquired//extends LockResult
  case object NoLockAvailable //extends LockResult
}

class LockManager extends Actor with ActorLogging {
  import LockManager._

  val locks = mutable.Map.empty[Int, Option[Cancellable]]

  def receive = {
    case RequestLock(id, autoReleaseAfter) ⇒ {
      val hashCode = id.hashCode
      if (locks contains (hashCode))
        sender ! NoLockAvailable
      else {
        aquireLock(id, autoReleaseAfter)
      }
    }
    case ReleaseLock(id) ⇒ releaseLock(id)

    case LockTimeout(id) ⇒ {
      lockTimeout(id)
      releaseLock(id)
    }
  }

  /** This can be overridden with preferred behavior */
  def lockTimeout(id: Any) {
    log.error(s"Lock timeout for id: $id")
  }

  def releaseLock(id: Any) {
    locks get id.hashCode match {
      case Some(lock) ⇒ {
        lock match {
          case Some(lockTimeout) ⇒ lockTimeout.cancel
          case None              ⇒
        }
        locks remove id.hashCode
      }
      case None ⇒ log.error(s"Tried to remove a lock that doesn't exist for id: $id")
    }
  }

  private def aquireLock(id: Any, autoReleaseAfter: Option[FiniteDuration]) {
    val lockTimeout = autoReleaseAfter match {
      case Some(delay) ⇒ Some(context.system.scheduler.scheduleOnce(delay, self, LockTimeout(id)))
      case _           ⇒ None
    }
    locks put (id.hashCode, lockTimeout)
    log.info(s"Aquired Lock for id: $id")
    sender ! LockAquired
  }
}