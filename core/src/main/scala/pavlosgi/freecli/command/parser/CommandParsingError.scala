package pavlosgi.freecli.command.parser

import cats.data.NonEmptyList

import pavlosgi.freecli.command.api.CommandField
import pavlosgi.freecli.config.parser.ConfigParsingError
import pavlosgi.freecli.core.formatting._
import pavlosgi.freecli.parser.Error

sealed trait CommandParsingError {
  val message: String
}

object CommandParsingError {
  implicit object errorInstance extends Error[CommandParsingError] {
    def message(error: CommandParsingError): String = {
      error.message
    }
  }
}

case class AdditionalArgumentsFound(args: Seq[String])
  extends CommandParsingError  {

  val message: String =
    s"Additional arguments found: ${args.mkString(", ")}"
}

case class FailedToParseConfig(
  field: CommandField,
  configErrors: NonEmptyList[ConfigParsingError])
  extends CommandParsingError  {

  val message: String =
    s"${field.shortDescription.yellow} command config errors, " +
    s"${configErrors.map(_.message).toList.mkString(", ")}"

}

case class CommandNotFound(field: CommandField)
  extends CommandParsingError  {

  val message: String =
    s"${field.shortDescription.yellow} command not found"
}

case object NoCommandWasMatched extends CommandParsingError {
  override val message: String = "No command was matched"
}

case object MultipleCommandsMatched extends CommandParsingError {
  override val message: String = "Multiple commands matched"
}