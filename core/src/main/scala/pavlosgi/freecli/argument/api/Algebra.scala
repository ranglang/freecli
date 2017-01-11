package pavlosgi.freecli.argument.api

import pavlosgi.freecli.core.api.StringDecoder

sealed trait Algebra[A]

final case class Arg[T, A](
  details: ArgumentField,
  f: T => A,
  g: StringDecoder[T])
  extends Algebra[A]