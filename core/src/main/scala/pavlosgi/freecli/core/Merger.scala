package pavlosgi.freecli.core

case class Merger[F](f: F) {
  def ::[F2](merger: Merger[F2])(implicit ev: Merger.CanMerge[F2, F]): ev.Out = {
    ev(merger.f, f)
  }
}

object Merger {
  trait CanMerge[F1, F2] {
    type Out

    def apply(f1: F1, f2: F2): Out
  }
}
