package pavlosgi.freecli.core.interpreters.parser

import cats.data._
import cats.instances.all._
import cats.syntax.all._
import cats.~>

import pavlosgi.freecli.core.api.AlgebraDependency
import pavlosgi.freecli.core.api.command._
import pavlosgi.freecli.core.api.config.{Algebra => ConfigAlgebra}
import pavlosgi.freecli.core.interpreters.parser.config.{configAlgebraParser, ResultT => CResultT}
import pavlosgi.freecli.core.interpreters.ResultTS

package object command {
  type ResultT[A] = ResultTS[CommandParsingError, ParserState, A]

  def parseCommand[G](
    args: Seq[String])
   (dsl: G)
   (implicit ev: G => ResultT[Command]):
    ValidatedNel[CommandParsingError, Command] = {

    (for {
      r <- ev(dsl)
      _ <- validateEmptyArguments
    } yield r).value.runA(
      ParserState(Arguments(args), Seq.empty)).value.toValidated
  }

  implicit def commandAlgebraParser = {
    new Algebra[ResultT] {
      def partialCmd[P](
        field: CommandField,
        run: P => Unit):
        ResultT[PartialCommand[P]] = {

        for {
          _ <- findAndSetCommandArgs(field)
        } yield PartialCommand[P](p => Command(field, run(p)))
      }

      def partialCmdWithConfig[H[_], C[_], A, P](
        field: CommandField,
        config: H[A],
        run: A => P => Unit)
       (implicit ev: AlgebraDependency[ConfigAlgebra, ResultT, C],
        ev2: H[A] => C[A]):
        ResultT[PartialCommand[P]] = {

        for {
          _    <- findAndSetCommandArgs(field)
          conf <- ev.nat(ev2(config))

        } yield PartialCommand[P](p => Command(field, run(conf)(p)))
      }

      def partialParentCmd[P, G[_]](
        field: CommandField,
        subs: G[PartialCommand[P]])
       (implicit ev: G[PartialCommand[P]] => ResultT[PartialCommand[P]]):
        ResultT[PartialCommand[P]] = {

        for {
          cmdArgs <- findAndSetCommandArgs(field)
          partial <- ev(subs)
        } yield partial
      }

      def partialParentCmdWithConfig[H[_], C[_], A, P, G[_]](
        field: CommandField,
        config: H[A],
        subs: G[A => PartialCommand[P]])
       (implicit ev: AlgebraDependency[ConfigAlgebra, ResultT, C],
        ev2: H[A] => C[A],
        ev3: G[A => PartialCommand[P]] => ResultT[A => PartialCommand[P]]):
        ResultT[PartialCommand[P]] = {

        for {
          _       <- findAndSetCommandArgs(field)
          conf    <- ev.nat(ev2(config))
          partial <- ev3(subs)
        } yield partial(conf)
      }

      override def combineK[A](
        x: ResultT[A],
        y: ResultT[A]):
        ResultT[A] = {

        EitherT.apply(for {
          xS <- x.value
          yS <- y.value
        } yield {
          (xS, yS) match {
            case (Right(c1), Left(_)) => Right(c1)
            case (Left(_), Right(c2)) => Right(c2)
            case (Left(c1), Left(c2)) => Left(c1.combine(c2))
            case (Right(_), Right(_)) =>
              Left(NonEmptyList.of(MultipleCommandsMatched))
          }
        })
      }

      override def pure[A](x: A): ResultT[A] =
        EitherT.pure(x)

      override def empty[A]: ResultT[A] =
        ResultTS.leftNE(NoCommandWasMatched)

      override def ap[A, B](
        ff: ResultT[A => B])
       (fa: ResultT[A]):
        ResultT[B] = {

        EitherT.apply(for {
          ff1 <- ff.value
          fa1 <- fa.value
        } yield {
          fa1.toValidated.ap(ff1.toValidated).toEither
        })
      }
    }
  }

  implicit def configDependency = {
    new AlgebraDependency[ConfigAlgebra, ResultT, CResultT] {
      override def algebra: ConfigAlgebra[CResultT] = configAlgebraParser
      override def nat: CResultT ~> ResultT = {
        new (CResultT ~> ResultT) {
          def apply[A](fa: CResultT[A]): ResultT[A] = {
            val st =
              fa.value.transformS[ParserState](_.args, (t, a) =>
                t.copy(args = Arguments(a.args)))

            EitherT.apply(st.map(_.leftMap(_.map(FailedToParseConfig))))
          }
        }
      }
    }
  }

  def findAndSetCommandArgs(field: CommandField): ResultT[Unit] = {
    for {
      st <- ResultTS.get[CommandParsingError, ParserState]
      _    <- st.args.args.indexWhere(field.matches) match {
                case idx if idx === -1 =>
                  ResultTS.leftNE[CommandParsingError, ParserState, Unit](
                    CommandNotFound(field))

                case idx =>
                  ResultTS.set[CommandParsingError, ParserState](
                    st.copy(args = Arguments(st.args.args.drop(idx + 1))))
              }

    } yield ()
  }

  def validateEmptyArguments: ResultT[Unit] = {
    for {
      st <- ResultTS.get[CommandParsingError, ParserState]
      res  <- if (st.args.args.nonEmpty)
                ResultTS.leftNE[CommandParsingError, ParserState, Unit](
                  UnknownArgumentsCommandParsingError(st.args.args))

              else ResultTS.pure[CommandParsingError, ParserState, Unit](())

    } yield res
  }
}