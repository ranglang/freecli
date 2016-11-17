package pavlosgi.freecli.core.interpreters.parser.config

import cats.Show
import cats.syntax.show._

import pavlosgi.freecli.core.api.config._

sealed trait ParsingError {
  val description: String
}

object ParsingError {
  def fromStringDecoderError(error: StringDecoderError): StringDecoderParsingError =
    StringDecoderParsingError(error)

  implicit object showInstance extends Show[ParsingError] {
    override def show(f: ParsingError): String = f.description
  }
}

case class UnknownArgumentsParsingError(args: Seq[String])
  extends ParsingError  {

  val description = s"Unknown arguments passed ${args.mkString(", ")}"
}

case class OptionFieldMissingParsingError(field: Field)
  extends ParsingError  {

  val description = s"Field ${field.show} is missing"
}

case class OptionFieldValueMissingParsingError(field: Field)
  extends ParsingError  {

  val description = s"Field value for ${field.show} is missing"
}

case class StringDecoderParsingError(error: StringDecoderError)
  extends ParsingError  {

  val description =
    s"Failed to decode from string. ${error.message}"
}

case class ArgumentValueMissingParsingError(details: ArgumentDetails)
  extends ParsingError  {

  val description = s"Argument ${details.show} missing"
}