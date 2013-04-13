package kalmanb.gridlock

import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import scala.concurrent.duration._
import java.util.UUID
import akka.util.Timeout
import akka.dispatch.OnFailure
import akka.dispatch.OnFailure
import akka.actor.ActorLogging
import scala.concurrent.Future
import akka.dispatch.OnComplete

/**
 * Must remember to return the ReleaseLock when work is complete!!
 */
abstract class LockableActor(
  lockManager: ActorRef,
  lockWaitTimeout: FiniteDuration = 5 minutes)
    extends Actor with ActorLogging {

  import LockManager._
  val lockIdDefault = UUID.randomUUID
  def lockId(message: Any) = lockIdDefault
  /** Can override this */

  implicit def akkaLockTimeout: Timeout = lockWaitTimeout
  implicit def executionContext = context.dispatcher

  def receive = {
    case message: Any ⇒ {
      val future = for {
        result ← (lockManager ? RequestLock(lockId(message)))
      } yield {
        result match {
          case LockAcquired ⇒ {
            work(message, lockId(message)).onComplete(_ ⇒ releaseLock(lockId(message)))
          }
          case NoLockAvailable ⇒ noLock(message, lockId(message))
        }
      }
      future.onFailure {
        case e ⇒ {
          lockManager ! ReleaseLock(lockId(message)) // Just in case ...
          log.error(s"Timed out waiting for lock for message: $message, ${e.getMessage}, ${e.getStackTraceString}")
          noLock(message, lockId(message))
        }
      }
    }
  }

  /**
   *  Override this to with what to do once we have a lock
   *  Any work within here will be performed in the execution context for this Actor
   */
  def work(message: Any, id: Any): Future[Unit]

  /** Override this to with what to do if we could not acquire a lock */
  def noLock(message: Any, id: Any) {}

  def releaseLock(id: Any) {
    lockManager ! ReleaseLock(id)
  }

}