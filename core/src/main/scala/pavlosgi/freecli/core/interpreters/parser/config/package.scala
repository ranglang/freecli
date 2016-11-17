package pavlosgi.freecli.core.interpreters.parser

import cats.data._
import cats.instances.all._
import cats.syntax.all._

import pavlosgi.freecli.core.api.Description
import pavlosgi.freecli.core.api.config._
import pavlosgi.freecli.core.interpreters.ResultTS

package object config {
  type ResultT[A] = ResultTS[ParsingError, Arguments, A]

  def parseConfig[G, A](
    args: Seq[String])
   (dsl: G)
   (implicit ev: G => ResultT[A]):
    ValidatedNel[ParsingError, A] = {

    ResultTS.run(Arguments(args))(
      ev(dsl)) match {
        case (Arguments(Nil), res) => res.toValidated
        case (Arguments(argsLeft), res) =>
          val ers = res.fold(_.toList, _ => List.empty)
          Validated.invalid(
            NonEmptyList(UnknownArgumentsParsingError(argsLeft), ers))
      }
  }

  implicit object configAlgebraParser extends Algebra[ResultT[?]] {
    override def arg[T, A](
      details: ArgumentDetails,
      f: T => A,
      g: StringDecoder[T]):
      ResultT[A] = {

      for {
        args   <- ResultTS.get[ParsingError, Arguments]
        value  <- extractArgumentValue(details, args)
        res    <- ResultTS.fromValidated[StringDecoderError, Arguments, T](
                    g.apply(value)).leftMap(
                    _.map[ParsingError](ParsingError.fromStringDecoderError))

      } yield f(res)
    }

    override def requiredOpt[T, A](
      field: Field,
      f: T => A,
      g: StringDecoder[T],
      default: Option[T]):
      ResultT[A] = {

      def mapping(o: Option[T]): ResultT[A] = {
        o.orElse(default) match {
          case Some(s) =>
            ResultTS.right(f(s))

          case None =>
            ResultTS.leftNE(OptionFieldMissingParsingError(field))
        }
      }

      for {
        res <- opt(field, mapping, g)
        v <- res
      } yield v
    }

    override def opt[T, A](
      field: Field,
      f: Option[T] => A,
      g: StringDecoder[T]):
      ResultT[A] = {

      for {
        args   <- ResultTS.get[ParsingError, Arguments]
        value  <- extractOptionFieldAndValue(field, args)
        res    <- ResultTS.fromValidated[StringDecoderError, Arguments, Option[T]](
                    value.traverseU(v => g.apply(v)))
                    .leftMap(_.map[ParsingError](ParsingError.fromStringDecoderError))

      } yield f(res)
    }

    override def flag[A](
      field: Field,
      f: Boolean => A):
      ResultT[A] = {

      for {
        args   <- ResultTS.get[ParsingError, Arguments]
        value  <- extractOptionFieldIfExists(field, args)
      } yield f(value)
    }

    override def sub[G, A](
      description: Description,
      dsl: G)
     (implicit ev: G => ResultT[A]):
      ResultT[A] = ev(dsl)

    override def pure[A](x: A): ResultT[A] = ResultTS.pure(x)

    override def ap[A, B](
      ff: ResultT[A => B])
     (fa: ResultT[A]):
      ResultT[B] = {

      EitherT.apply[State[Arguments, ?], NonEmptyList[ParsingError], B](for {
        ff1 <- ff.value
        fa1 <- fa.value
      } yield fa1.toValidated.ap(ff1.toValidated).toEither)

    }
  }

  def extractOptionFieldIfExists(
    field: Field,
    args: Arguments):
    ResultT[Boolean] = {

    args.args.indexWhere(field.matches) match {
      case idx if idx === -1 =>
        tryBySplittingArgs(
          field,
          args,
          extractOptionFieldIfExists,
          ResultTS.right(false))

      case idx =>
        val remArgs = args.args.take(idx) ++ args.args.drop(idx + 1)
        ResultTS.set(Arguments(remArgs)).map(_ => true)
    }
  }

  def extractOptionFieldAndValue(
    field: Field,
    args: Arguments):
    ResultT[Option[String]] = {

    args.args.indexWhere(field.matches) match {
      case idx if idx === -1 =>
        tryBySplittingArgs(
          field,
          args,
          extractOptionFieldAndValue,
          ResultTS.right(None))

      case idx =>
        args.args.lift(idx + 1) match {
          case None =>
            ResultTS.leftNE(OptionFieldValueMissingParsingError(field))

          case Some(v) =>
            val remArgs = args.args.take(idx) ++ args.args.drop(idx + 2)
            ResultTS.set(Arguments(remArgs)).map(_ => Some(v))
        }
    }
  }

  def tryBySplittingArgs[T](
    field: Field,
    args: Arguments,
    f: (Field, Arguments) => ResultT[T],
    fallback: ResultT[T]) = {

    val newArgs = args.args.foldLeft(Seq.empty[String]) {
      case (curr, arg) =>
        curr ++ FieldAbbreviation.splitMultiFieldAbbreviation(arg)
    }

    if (newArgs.diff(args.args).isEmpty) {
      fallback
    } else {
      f(field, Arguments(newArgs))
    }
  }

  def extractArgumentValue(
    details: ArgumentDetails,
    args: Arguments):
    ResultT[String] = {

    args.args.headOption match {
      case None =>
        ResultTS.leftNE(ArgumentValueMissingParsingError(details))

      case Some(v) =>
        val remArgs = args.args.drop(1)
          ResultTS.set(Arguments(remArgs)).map(_ => v)
    }
  }
}