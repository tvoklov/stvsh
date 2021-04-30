package com.volk.stvsh.db

import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Schema.Key

object Aliases {

  type ID = String
  type SheetValues = Map[Key, SheetField]

}
