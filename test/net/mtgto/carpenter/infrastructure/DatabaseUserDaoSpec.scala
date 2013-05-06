package net.mtgto.carpenter.infrastructure

import java.util.UUID
import org.specs2.mutable.Specification
import play.api.test.WithApplication
import org.specs2.execute.{AsResult, Result}
import play.api.db.DB
import anorm._

class DatabaseUserDaoSpec extends Specification {
  trait Setup extends WithApplication {
    override def around[T: AsResult](t: => T): Result = super.around {
      // do something
      prepareDatabase
      t
    }

    val databaseUserDao: DatabaseUserDao = new DatabaseUserDao

    val testId: UUID = UUID.randomUUID

    val testNoExistId: UUID = UUID.randomUUID

    val testName = "test-user"

    val testPassword = "test-password"

    val testCanLogin = true

    val testCanCreateUser = true

    def prepareDatabase = {
      DB.withTransaction{ implicit c =>
        SQL("INSERT INTO `users` (`id`, `name`, `password`) VALUES ({id},{name},{password})")
          .on('id -> testId.toString, 'name -> testName, 'password -> testPassword).executeInsert()
        SQL("INSERT INTO `authorities` (`user_id`, `can_login`, `can_create_user`) VALUES ({userId},{canLogin},{canCreateUser})")
          .on('userId -> testId.toString, 'canLogin -> testCanLogin, 'canCreateUser -> testCanCreateUser).executeInsert()
      }
    }
  }

  "findById" should {
    "get None if there is no user which has specified id" in new Setup {
      databaseUserDao.findById(testNoExistId) must beNone
    }

    "get one user record if there is one which has specified id" in new Setup {
      databaseUserDao.findById(testId) must beSome.which( user =>
        user.id == testId && user.name == testName && user.authority.canLogin == testCanLogin && user.authority.canCreateUser == testCanCreateUser
      )
    }
  }

  "findByNameAndPassword" should {
    "get None if there is no user which has specified name and password" in new Setup {
      databaseUserDao.findByNameAndPassword(testName, testPassword + "0") must beNone
      databaseUserDao.findByNameAndPassword(testName + "0", testPassword) must beNone
    }

    "get one user record if there is one which has specified name and password" in new Setup {
      databaseUserDao.findByNameAndPassword(testName, testPassword) must beSome.which( user =>
        user.id == testId && user.name == testName && user.authority.canLogin == testCanLogin && user.authority.canCreateUser == testCanCreateUser
      )
    }
  }
}
