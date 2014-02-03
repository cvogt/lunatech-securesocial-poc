package plugins

import play.api.Application
import securesocial.core.{IdentityId, UserId, Identity, UserServicePlugin}
import securesocial.core.providers.Token
import models.Tables._

class UserService(application: Application) extends UserServicePlugin(application) {

  def find(id: IdentityId) = Users.findByIdentityId(id)
  def save(user: Identity) = Users.save(user)

  // Since we're not using username/password login, we don't need the methods below
  def findByEmailAndProvider(email: String, providerId: String) = None
  def save(token: Token) {}

  def findToken(token: String) = None

  def deleteToken(uuid: String) {}

  def deleteExpiredTokens() {}
}
