package com.receipty.bantu.service.test

import com.receipty.bantu.core.db.mysql.cache.{ItemDbCacheT, UserDbCacheT}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import com.receipty.bantu.service.util.ReceiptyUtils

object ItemDbCache extends ItemDbCacheT{
  override def getUserItems(uid: Int): List[MysqlDbService.ItemDbEntry] = {
    List(ItemDbEntry(
      id = 0,
      description = "pick and peel",
      owner = 5,
      added = ""
    ),
      ItemDbEntry(
        id = 0,
        description = "strawberry",
        owner = 5,
        added = ""
      ),
      ItemDbEntry(
        id = 0,
        description = "jello",
        owner = 5,
        added = ""
      ),
      ItemDbEntry(
        id = 0,
        description = "spaghetti",
        owner = 5,
        added = ""
      ))
  }

}
object UserDbCache extends UserDbCacheT {
  override def checkIfUserExists(phoneNumber: String): Option[MysqlDbService.UserDbEntry] = {
    Some(UserDbEntry(
      id          = 5,
      phoneNumber = "+2348112172624",
      password    = ReceiptyUtils.hashPassword("1234"),
      province    = 3,
      county      = 3
    ))
  }
}
