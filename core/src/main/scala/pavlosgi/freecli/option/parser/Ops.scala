package pavlosgi.freecli.option.parser

import cats.data.{NonEmptyList, Validated}
import cats.syntax.all._

import pavlosgi.freecli.option.dsl.OptionDsl
import pavlosgi.freecli.parser.CliFailure
import pavlosgi.freecli.{parser => P}

trait Ops {
  def parseOptions[A](
    args: Seq[String])
   (dsl: OptionDsl[A]):
    Validated[CliFailure[OptionParsingError], A] = {

    val (arguments, res) =
      P.CliParser.run(args)(dsl.foldMap(OptionParserInterpreter))

    arguments.usable match {
      case Nil => res.toValidated
      case u =>
        val error = CliFailure.errors[OptionParsingError](
          NonEmptyList.of(AdditionalArgumentsFound(u.map(_.name))))

        res match {
          case Left(failure) =>
            Validated.invalid(failure.combine(error))

          case Right(_) =>
            Validated.invalid(error)
        }
    }
  }
}