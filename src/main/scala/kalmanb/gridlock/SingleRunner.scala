package kalmanb.gridlock

import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import scala.concurrent.duration._
import java.util.UUID
import akka.util.Timeout
import akka.actor.ActorLogging
import scala.concurrent.Future

/**
 * This is designed to only run a single long running process at a time.
 * If it cannot acquire a lock because it is already running or the lock
 * request times out the message will be dropped.
 *
 * Often used for repeated tasks that get run regularly
 */
abstract class SingleRunner(
  lockManager: ActorRef,
  lockWaitTimeout: FiniteDuration = 5 minutes)
    extends Actor with ActorLogging {

  import LockManager._

  val lockIdDefault = UUID.randomUUID
  /** Can override this */
  def lockId(message: Any) = lockIdDefault

  implicit def akkaLockTimeout: Timeout = lockWaitTimeout
  implicit def executionContext = context.dispatcher

  def receive = {
    case message: Any ⇒
      for {
        result ← (lockManager ? RequestLock(lockId(message)))
      } yield result match {
        case LockAcquired ⇒
          work(message, lockId(message)).onComplete(_ => releaseLock(lockId(message)))
      }
  }

  /** Override this to with what to do once we have a lock */
  def work(message: Any, id: Any):Future[Unit]

  def releaseLock(id: Any) {
    lockManager ! ReleaseLock(id)
  }
}