package models

import securesocial.core._
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.{ToShapedValue, ProvenShape}

import Tables._
case class User(uid: Option[Long] = None,
                identityId: IdentityId,
                firstName: String,
                lastName: String,
                fullName: String,
                email: Option[String],
                avatarUrl: Option[String],
                authMethod: AuthenticationMethod,
                oAuth1Info: Option[OAuth1Info],
                oAuth2Info: Option[OAuth2Info],
                passwordInfo: Option[PasswordInfo] = None) extends Identity {

}

object UserFromIdentity{
 def apply(i: Identity): User = User(None, i.identityId, i.firstName, i.lastName, i.fullName,
   i.email, i.avatarUrl, i.authMethod, i.oAuth1Info, i.oAuth2Info)
}

class Users(tag: Tag) extends Table[User](tag, "user") {

  implicit def string2AuthenticationMethod = MappedColumnType.base[AuthenticationMethod, String](
    authenticationMethod => authenticationMethod.method,
    string => AuthenticationMethod(string)
  )

  implicit def tuple2OAuth1Info(tuple: (Option[String], Option[String])): Option[OAuth1Info] = tuple match {
    case (Some(token), Some(secret)) => Some(OAuth1Info(token, secret))
    case _ => None
  }

  implicit def tuple2OAuth2Info(tuple: (Option[String], Option[String], Option[Int], Option[String])): Option[OAuth2Info] = tuple match {
    case (Some(token), tokenType, expiresIn, refreshToken) => Some(OAuth2Info(token, tokenType, expiresIn, refreshToken))
    case _ => None
  }

  implicit def tuple2IdentityId(tuple: (String, String)): IdentityId = tuple match {
    case (userId, providerId) => IdentityId(userId, providerId)
  }

  def uid = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId")

  def providerId = column[String]("providerId")

  def email = column[Option[String]]("email")

  def firstName = column[String]("firstName")

  def lastName = column[String]("lastName")

  def fullName = column[String]("fullName")

  def authMethod = column[AuthenticationMethod]("authMethod")

  def avatarUrl = column[Option[String]]("avatarUrl")

  // oAuth 1
  def token = column[Option[String]]("token")

  def secret = column[Option[String]]("secret")

  // oAuth 2
  def accessToken = column[Option[String]]("accessToken")

  def tokenType = column[Option[String]]("tokenType")

  def expiresIn = column[Option[Int]]("expiresIn")

  def refreshToken = column[Option[String]]("refreshToken")

  //def f = User.tupled

  def g = {
    (u: User) => Some((u.uid, u.identityId.userId, u.identityId.providerId, u.firstName, u.lastName, u.fullName, u.email,
      u.avatarUrl, u.authMethod, u.oAuth1Info.map(_.token), u.oAuth1Info.map(_.secret), u.oAuth2Info.map(_.accessToken),
      u.oAuth2Info.flatMap(_.tokenType), u.oAuth2Info.flatMap(_.expiresIn), u.oAuth2Info.flatMap(_.refreshToken)))
  }

  def * : ProvenShape[User] = {
    val shapedValue = (uid.?,
      userId,
      providerId,
      firstName,
      lastName,
      fullName,
      email,
      avatarUrl,
      authMethod,
      token,
      secret,
      accessToken,
      tokenType,
      expiresIn,
      refreshToken
    ).shaped

    shapedValue.<>(u => User.apply(uid = u._1,
      identityId = tuple2IdentityId(u._2, u._3),
      firstName = u._4,
      lastName = u._5,
      fullName = u._6,
      email = u._7,
      avatarUrl = u._8,
      authMethod = u._9,
      oAuth1Info = (u._10, u._11),
      oAuth2Info = (u._12, u._13, u._14, u._15)), g)
  }

}

object Tables{
  val Users = new TableQuery[Users](new Users(_)){
    def autoInc = this returning this.map(_.uid)

    def findById(id: Long) = withSession {
      implicit session =>
        val q = for {
          user <- this
          if user.uid is id
        } yield user

        q.firstOption
    }

    def findByIdentityId(identityId: IdentityId): Option[User] = withSession {
      implicit session =>
        val q = for {
          user <- this
          if (user.userId is identityId.userId) && (user.providerId is identityId.providerId)
        } yield user

        q.firstOption
    }

    def all = withSession {
      implicit session =>
        val q = for {
          user <- this
        } yield user

        q.list
    }

    def save(i: Identity): User = this.save(UserFromIdentity(i))

    def save(user: User): User = withSession {
      implicit session =>
        findByIdentityId(user.identityId) match {
          case None => {
            val uid = this.autoInc.insert(user)
            user.copy(uid = Some(uid))
          }
          case Some(existingUser) => {
            val userRow = for {
              u <- this
              if u.uid is existingUser.uid
            } yield u

            val updatedUser = user.copy(uid = existingUser.uid)
            userRow.update(updatedUser)
            updatedUser
          }
        }
    }

    def withSession[T](block: (Session => T)) =
      Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withSession {
        session =>
          block(session)
      }
  }
}