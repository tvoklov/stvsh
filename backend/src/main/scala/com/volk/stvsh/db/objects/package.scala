package com.volk.stvsh.db

import doobie.util.fragment.Fragment

package object objects {

  def asFragment: String => Fragment = Fragment.const(_)

}
