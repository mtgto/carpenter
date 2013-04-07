package net.mtgto.infrastructure

import anorm._
import anorm.SqlParser._
import play.api.db.DB

import java.util.UUID

class DatabaseUserDao extends UserDao {
  import play.api.Play.current

  protected[this] def convertRowToUser(row: Row): User = {
    row match {
      case Row(id: String, name: String, canLogin: Byte, canCreateUser: Byte) =>
        User(UUID.fromString(id), name, Authority(canLogin != 0, canCreateUser != 0))
    }
  }

  override def findById(id: UUID): Option[User] = {
    DB.withConnection{ implicit c =>
      SQL("""SELECT `users`.`id`, `users`.`name`, `authorities`.`can_login`, `authorities`.`can_create_user` 
            |FROM `users` INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id` WHERE `users`.`id` = {id}"""
      .stripMargin)
      .on('id -> id.toString)()
      .headOption.map(convertRowToUser)
    }
  }

  override def findByNameAndPassword(name: String, password: String): Option[User] = {
    DB.withConnection{ implicit c =>
      SQL("""SELECT `users`.`id`, `users`.`name`, `authorities`.`can_login`, `authorities`.`can_create_user` 
            |FROM `users` INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id` 
            |WHERE `name` = {name} AND `password` = {password}""".stripMargin)
      .on('name -> name, 'password -> password)()
      .headOption.map(convertRowToUser)
    }
  }

  override def findAll: Seq[User] = {
    DB.withConnection{ implicit c =>
      SQL("""SELECT `users`.`id`, `users`.`name`, `authorities`.`can_login`, `authorities`.`can_create_user` 
            |FROM `users` INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`""".stripMargin)()
      .map(convertRowToUser).toList
    }
  }

  override def save(id: UUID, name: String, password: String, authority: Authority): Unit = {
    DB.withTransaction{ implicit c =>
      val rowCount =
        SQL("UPDATE `users` SET `name` = {name}, `password` = {password} WHERE `id` = {id}")
          .on('id -> id.toString, 'name -> name, 'password -> password).executeUpdate()
      if (rowCount == 0)
        SQL("INSERT INTO `users` (`id`, `name`,`password`) VALUES ({id},{name},{password})")
          .on('id -> id.toString, 'name -> name, 'password -> password).executeInsert()
      val subRowCount =
        SQL("UPDATE `authorities` SET `can_login` = {canLogin}, `can_create_user` = {canCreateUser} WHERE `user_id` = {userId}")
          .on('userId -> id.toString, 'canLogin -> authority.canLogin, 'canCreateUser -> authority.canCreateUser).executeUpdate()
      if (subRowCount == 0)
        SQL("INSERT INTO `authorities` (`user_id`, `can_login`,`can_create_user`) VALUES ({userId},{canLogin},{canCreateUser})")
          .on('userId -> id.toString, 'canLogin -> authority.canLogin, 'canCreateUser -> authority.canCreateUser).executeInsert()
    }
  }

  override def delete(id: UUID): Int = {
    DB.withTransaction{ implicit c =>
      SQL("DELETE `authorities` WHERE `user_id` = {userId}")
        .on('userId -> id.toString).executeUpdate()
      SQL("DELETE `users` WHERE `id` = {id}")
        .on('id -> id.toString).executeUpdate()
    }
  }
}
