package freecli
package option
package parser

import cats.data.NonEmptyList

import option.api.Action
import option.dsl.OptionDsl
import freecli.parser.{CliArgument, CliParser}

object ops extends ParserOps
trait ParserOps {
  private[freecli] def parseOptionNonStrict[T](
    dsl: OptionDsl[T],
    lookAhead: Boolean = true):
    CliParser[Action, OptionParsingErrors, T] = {

    def unusedOutOfOrder(args: Seq[CliArgument]) = {
      val l = args.map(a => (a, !a.isUsable)).dropWhile(_._2)
      l.take(l.indexWhere(_._2)).map(_._1)
    }

    for {
      r <- dsl.foldMap(OptionParserInterpreter)
      args <- CliParser.getArgs[Action, OptionParsingErrors]
      _ <- if (lookAhead) {
        CliParser.success[Action, OptionParsingErrors, Unit](())
      } else {
        unusedOutOfOrder(args) match {
          case Nil => CliParser.success[Action, OptionParsingErrors, Unit](())
          case l   => CliParser.error[Action, OptionParsingErrors, Unit](
            NonEmptyList.of(ArgumentsDidNotMatchAnyOptions(l.map(_.name))))
        }
      }
    } yield r
  }

  def parseOption[T](
    dsl: OptionDsl[T],
    lookAhead: Boolean = true):
    CliParser[Action, OptionParsingErrors, T] = {

    parseOptionNonStrict(dsl, lookAhead).failIfNotAllArgumentsUsed(
      args => NonEmptyList.of(AdditionalArgumentsFound(args.map(_.name))))
  }
}
