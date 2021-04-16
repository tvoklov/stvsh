package com.volk.stvsh.extensions

import doobie.util.fragment.Fragment

object Sql {

  implicit class SqlFixer(inSqlString: String) {
    def fixForSql: String =
      inSqlString.flatMap {
        case '\'' => "''"
        case x    => x + ""
      }
  }

  implicit class ToSqlFragment(sqlString: String) {
    def toFragment: Fragment = Fragment.const(sqlString)
  }

}
