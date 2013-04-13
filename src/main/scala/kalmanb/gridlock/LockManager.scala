package kalmanb.gridlock

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.Cancellable

object LockManager {
  case class RequestLock(id: Any, autoReleaseAfter: Option[FiniteDuration] = None, acquireTimeOut:Option[FiniteDuration] = None)
  case class ReleaseLock(id: Any)
  case class LockTimeout(id: Any)
  
  case object LockAcquired
  case object NoLockAvailable
  
  def hash(lockId: Any) = lockId.hashCode + lockId.getClass.getName.hashCode
}

class LockManager extends Actor with ActorLogging {
  import LockManager._

  val locks = mutable.Map.empty[Int, Option[Cancellable]]

  def receive = {
    case RequestLock(id, autoReleaseAfter, acquireTimeout) ⇒ {
      val hashCode = hash(id)
      if (locks contains (hashCode))
        sender ! NoLockAvailable
      else {
        acquireLock(id, autoReleaseAfter)
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
    val hashCode = hash(id)
    locks get hashCode match {
      case Some(lock) ⇒ {
        lock match {
          case Some(lockTimeout) ⇒ lockTimeout.cancel
          case None              ⇒
        }
        locks remove hashCode
      }
      case None ⇒ log.error(s"Tried to remove a lock that doesn't exist for id: $id")
    }
  }

  private def acquireLock(id: Any, autoReleaseAfter: Option[FiniteDuration]) {
    val lockTimeout = autoReleaseAfter match {
      case Some(delay) ⇒ Some(context.system.scheduler.scheduleOnce(delay, self, LockTimeout(id)))
      case _           ⇒ None
    }
    locks put (hash(id), lockTimeout)
    log.debug(s"Aquired Lock for id: $id")
    sender ! LockAcquired
  }
}