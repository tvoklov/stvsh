package com.volk.stvsh.extensions

import doobie.Fragment

object Doobie {

  def combine(fragments: Iterable[Fragment]): Fragment =
    fragments.foldLeft(Fragment.empty)(
      (p, f) => p ++ Fragment.const("\n") ++ f
    )

  implicit class FragmentCombiner(fragments: Iterable[Fragment]) {
    def combine: Fragment = Doobie.combine(fragments)
  }

}
