package freecli
package command
package dsl

import shapeless.{::, HList, HNil}

import api.{PartialCommand, RunCommand}
import config.dsl.ConfigDsl
import core.free.FreeAlternative
import core.api.{Merger, CanMerge}

trait MergerImplicits {
  implicit def commandDslBuilder2Merger[H <: HList, O, A](
    b: CommandDslBuilder[H, O, A]):
    Merger[CommandDslBuilder[H, O, A]] = {

    Merger(b)
  }

  implicit def canMergeConfigToGenericRun[C1, R1, C2, R2]:
    CanMerge.Aux[
      CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
      CommandDslBuilder[RunCommand[R2] :: HNil, C2, R2],
      CommandDslBuilder[ConfigDsl[C1] :: RunCommand[R2] :: HNil, C1, R2]] = {

    new CanMerge[
      CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
      CommandDslBuilder[RunCommand[R2] :: HNil, C2, R2]] {

      type Out = CommandDslBuilder[ConfigDsl[C1] :: RunCommand[R2] :: HNil, C1, R2]

      def apply(
        h1: CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
        h2: CommandDslBuilder[RunCommand[R2] :: HNil, C2, R2]):
        Out = {

        CommandDslBuilder[ConfigDsl[C1] :: RunCommand[R2] :: HNil, C1, R2](
          h1.list.head :: h2.list.head :: HNil)
      }
    }
  }

  implicit def canMergeConfigToPartial[C1, R1, C2, R2]:
    CanMerge.Aux[
      CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
      CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, C2, R2],
      CommandDslBuilder[ConfigDsl[C1] :: CommandDsl[PartialCommand[R2]] :: HNil, C1, R2]] = {

    new CanMerge[
      CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
      CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, C2, R2]] {

      type Out = CommandDslBuilder[ConfigDsl[C1] :: CommandDsl[PartialCommand[R2]] :: HNil, C1, R2]

      def apply(
        h1: CommandDslBuilder[ConfigDsl[C1] :: HNil, C1, R1],
        h2: CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, C2, R2]):
        Out = {

        CommandDslBuilder[ConfigDsl[C1] :: CommandDsl[PartialCommand[R2]] :: HNil, C1, R2](
          h1.list.head :: h2.list.head :: HNil)
      }
    }
  }

  implicit def canMergePartialToPartial[R1, R2, RH <: HList](
    implicit ev: ToFromHList[R1, RH],
    ev2: ToFromHList[R2, RH]):
    CanMerge.Aux[
      CommandDslBuilder[CommandDsl[PartialCommand[R1]] :: HNil, HNil, R1],
      CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, HNil, R2],
      CommandDslBuilder[CommandDsl[PartialCommand[RH]] :: HNil, HNil, RH]] = {

    new CanMerge[
      CommandDslBuilder[CommandDsl[PartialCommand[R1]] :: HNil, HNil, R1],
      CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, HNil, R2]] {

      type Out = CommandDslBuilder[CommandDsl[PartialCommand[RH]] :: HNil, HNil, RH]

      def apply(
        h1: CommandDslBuilder[CommandDsl[PartialCommand[R1]] :: HNil, HNil, R1],
        h2: CommandDslBuilder[CommandDsl[PartialCommand[R2]] :: HNil, HNil, R2]):
        Out = {

        val dsl =
          FreeAlternative.combineK(
            h1.list.head.map(
              partial => PartialCommand[RH](r => partial.f(ev.from(r)))),

            h2.list.head.map(
              partial => PartialCommand[RH](r => partial.f(ev2.from(r)))))

        CommandDslBuilder(dsl :: HNil)
      }
    }
  }
}

