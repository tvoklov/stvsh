package com.volk.stvsh.extensions

object SqlUtils {

  implicit class SqlFixer(inSqlString: String) {
    def fixForSql: String =
      inSqlString.flatMap {
        case '\'' => "''"
        case x    => x + ""
      }
  }

}
