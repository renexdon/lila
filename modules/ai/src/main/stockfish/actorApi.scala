package lila.ai
package stockfish
package actorApi

import akka.actor.ActorRef

case class AddTime(time: Int)

sealed trait State
case object Starting extends State
case object Idle extends State
case object IsReady extends State
case object Running extends State

sealed trait Stream { def text: String }
case class Out(text: String) extends Stream
case class Err(text: String) extends Stream

sealed trait Req {
  def moves: List[String]
  def fen: Option[String]
  def analyse: Boolean

  def chess960 = fen.isDefined
}

case class PlayReq(
    moves: List[String],
    fen: Option[String],
    level: Int) extends Req {

  def analyse = false
}

case class AnalReq(
    moves: List[String],
    fen: Option[String]) extends Req {

  def analyse = true

  def isStart = moves.isEmpty && fen.isEmpty
}

case class FullAnalReq(moves: List[String], fen: Option[String])

case class Job(req: Req, sender: akka.actor.ActorRef, buffer: List[String]) {

  def +(str: String) = req.analyse.fold(copy(buffer = str :: buffer), this)

  // bestmove xyxy ponder xyxy
  def complete(str: String): Option[Any] = req match {
    case r: PlayReq ⇒ str split ' ' lift 1
    case AnalReq(moves, _) ⇒ buffer.headOption map EvaluationParser.apply
  }
}
