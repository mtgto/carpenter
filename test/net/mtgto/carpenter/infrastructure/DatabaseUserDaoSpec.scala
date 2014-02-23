package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.test.WithApplication
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.Specification
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

class DatabaseUserDaoSpec extends Specification {
  abstract class Setup extends WithApplication {
    override def around[T](t : => T)(implicit evidence : AsResult[T]): Result = super.around {
      // do something
      prepareDatabase
      t
    }

    lazy val databaseUserDao: DatabaseUserDao = new DatabaseUserDao

    lazy val testId: UUID = UUID.randomUUID

    lazy val testNoExistId: UUID = UUID.randomUUID

    lazy val testName = "test-user"

    lazy val testPassword = "test-password"

    lazy val testCanLogin = true

    lazy val testCanCreateUser = true

    def prepareDatabase = {
      DB.withTransaction{ implicit session: Session =>
        TableQuery[Users] += User(testId.toString, testName, testPassword)
        TableQuery[Authorities] += Authority(testId.toString, testCanLogin, testCanCreateUser)
      }
    }
  }

  "findById" should {
    "get None if there is no user which has specified id" in new Setup {
      databaseUserDao.findById(testNoExistId.toString) must beNone
    }

    "get one user record if there is one which has specified id" in new Setup {
      databaseUserDao.findById(testId.toString) must beSome.which {
        case (user, authority) =>
          user.id == testId.toString && user.name == testName && authority.canLogin == testCanLogin && authority.canCreateUser == testCanCreateUser
        }
    }
  }

  "findByNameAndPassword" should {
    "get None if there is no user which has specified name and password" in new Setup {
      databaseUserDao.findByNameAndPassword(testName, testPassword + "0") must beNone
      databaseUserDao.findByNameAndPassword(testName + "0", testPassword) must beNone
    }

    "get one user record if there is one which has specified name and password" in new Setup {
      databaseUserDao.findByNameAndPassword(testName, testPassword) must beSome.which {
        case (user, authority) =>
          user.name == testName && authority.canLogin == testCanLogin && authority.canCreateUser == testCanCreateUser
      }
    }
  }
}
