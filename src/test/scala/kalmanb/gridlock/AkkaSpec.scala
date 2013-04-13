package kalmanb.gridlock

import org.scalatest.Finders
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit

abstract class AkkaSpec extends TestKit(ActorSystem("AkkaTestSystem"))
  with FunSpec
  with ShouldMatchers
  with ImplicitSender