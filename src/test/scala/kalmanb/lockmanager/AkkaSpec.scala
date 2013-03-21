package kalmanb.lockmanager

import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec
import akka.actor.ActorSystem

abstract class AkkaSpec extends TestKit(ActorSystem("AkkaTestSystem"))
  with ImplicitSender
  with FunSpec
  with ShouldMatchers