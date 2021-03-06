package com.blinkbox.books.auth.server.data

import com.blinkbox.books.auth.UserRole
import com.blinkbox.books.auth.UserRole.UserRole
import com.blinkbox.books.auth.server.sso.{SsoRefreshToken, SsoUserId}
import com.blinkbox.books.slick.TablesContainer
import org.joda.time.DateTime

import scala.reflect.ClassTag
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.{MappedProjection, ProvenShape}

trait ZuulTables[Profile <: JdbcProfile] extends TablesContainer[Profile] {
  import driver.simple._

  lazy val users = TableQuery[Users]
  lazy val clients = TableQuery[Clients]
  lazy val refreshTokens = TableQuery[RefreshTokens]
  lazy val loginAttempts = TableQuery[LoginAttempts]
  lazy val privileges = TableQuery[Privileges]
  lazy val roles = TableQuery[Roles]
  lazy val previousUsernames = TableQuery[PreviousUsernames]

  implicit lazy val userIdColumnType = MappedColumnType.base[UserId, Int](_.value, UserId(_))
  implicit lazy val clientIdColumnType = MappedColumnType.base[ClientId, Int](_.value, ClientId(_))
  implicit lazy val refreshTokenIdColumnType = MappedColumnType.base[RefreshTokenId, Int](_.value, RefreshTokenId(_))
  implicit lazy val ssoIdColumnType = MappedColumnType.base[SsoUserId, String](_.value, SsoUserId(_))
  implicit lazy val ssoRefreshTokenColumnType = MappedColumnType.base[SsoRefreshToken, String](_.value, SsoRefreshToken(_))
  implicit lazy val roleIdColumnType = MappedColumnType.base[RoleId, Int](_.value, RoleId(_))
  implicit lazy val privilegeIdColumnType = MappedColumnType.base[PrivilegeId, Int](_.value, PrivilegeId(_))
  implicit lazy val userRoleColumnType = MappedColumnType.base[UserRole, String](
    _.toString, s => UserRole.values.find(_.toString == s.toString).getOrElse(UserRole.NotUnderstood))
  implicit lazy val previousUsernameIdColumnType = MappedColumnType.base[PreviousUsernameId, Int](_.value, PreviousUsernameId(_))

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[UserId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def firstName = column[String]("first_name", O.NotNull)
    def lastName = column[String]("last_name", O.NotNull)
    def passwordHash = column[String]("password_hash", O.NotNull)
    def allowMarketing = column[Boolean]("allow_marketing_communications", O.NotNull)
    def ssoId = column[Option[SsoUserId]]("sso_id", O.Default(None))
    def * = (id, createdAt, updatedAt, username, firstName, lastName, passwordHash, allowMarketing, ssoId) <> (User.tupled, User.unapply)
    def indexOnUsername = index("index_users_on_username", username, unique = true)
  }

  class Roles(tag: Tag) extends Table[Role](tag, "user_roles") {
    def id = column[RoleId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def name = column[UserRole]("name", O.NotNull)
    def description = column[String]("description", O.NotNull)

    def * = (id, name, description) <> (Role.tupled, Role.unapply)
  }

  class Privileges(tag: Tag) extends Table[(Privilege)](tag, "user_privileges") {
    def id = column[PrivilegeId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def userId = column[UserId]("user_id", O.NotNull)
    def roleId = column[RoleId]("user_role_id", O.NotNull)
    def user = foreignKey("fk_privileges_to_users", userId, users)(_.id)
    def role = foreignKey("fk_privileges_to_roles", roleId, roles)(_.id)

    def * = (id, createdAt, userId, roleId) <> (Privilege.tupled, Privilege.unapply)
  }

  class Clients(tag: Tag) extends Table[Client](tag, "clients") {
    def id = column[ClientId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[UserId]("user_id", O.NotNull)
    def name = column[String]("name", O.NotNull)
    def brand = column[String]("brand", O.NotNull)
    def model = column[String]("model", O.NotNull)
    def os = column[String]("os", O.NotNull)
    def secret = column[String]("client_secret", O.NotNull)
    def isDeregistered = column[Boolean]("deregistered", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, name, brand, model, os, secret, isDeregistered) <> (Client.tupled, Client.unapply)
    def indexOnUserId = index("index_clients_on_user_id", userId)
  }

  class RefreshTokens(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {
    def id = column[RefreshTokenId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def updatedAt = column[DateTime]("updated_at", O.NotNull)
    def userId = column[UserId]("user_id", O.NotNull)
    def clientId = column[Option[ClientId]]("client_id")
    def token = column[String]("token", O.NotNull)
    def ssoToken = column[Option[SsoRefreshToken]]("sso_refresh_token")
    def isRevoked = column[Boolean]("revoked", O.NotNull)
    def expiresAt = column[DateTime]("expires_at", O.NotNull)
    def elevationExpiresAt = column[DateTime]("elevation_expires_at", O.NotNull)
    def criticalElevationExpiresAt = column[DateTime]("critical_elevation_expires_at", O.NotNull)
    def * = (id, createdAt, updatedAt, userId, clientId, token, ssoToken, isRevoked, expiresAt, elevationExpiresAt, criticalElevationExpiresAt) <> (RefreshToken.tupled, RefreshToken.unapply)
    def indexOnToken = index("index_refresh_tokens_on_token", token)
    def user = foreignKey("fk_refresh_tokens_to_users", userId, users)(_.id)
  }

  class LoginAttempts(tag: Tag) extends Table[LoginAttempt](tag, "login_attempts") {
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def successful = column[Boolean]("successful", O.NotNull)
    def clientIP = column[String]("client_ip", O.NotNull)
    def * = (createdAt, username, successful, clientIP) <> (LoginAttempt.tupled, LoginAttempt.unapply)
    def indexOnUsernameAndCreatedAt = index("index_login_attempts_on_username_and_created_at", (username, createdAt))
  }

  class PreviousUsernames(tag: Tag) extends Table[PreviousUsername](tag, "user_previous_usernames") {
    def id = column[PreviousUsernameId]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def createdAt = column[DateTime]("created_at", O.NotNull)
    def userId = column[UserId]("user_id", O.NotNull)
    def username = column[String]("username", O.NotNull)
    def * = (id, createdAt, userId, username) <> (PreviousUsername.tupled, PreviousUsername.unapply)
    def user = foreignKey("fk_user_previous_username_to_users", userId, users)(_.id)

    type OptionColumns = (Option[PreviousUsernameId], Option[DateTime], Option[UserId], Option[String])

    def ? =
      (id.?, createdAt.?, userId.?, username.?).<>[Option[PreviousUsername], OptionColumns](
        {
          case (Some(id), Some(createdAt), Some(userId), Some(username)) => Option(PreviousUsername(id, createdAt, userId, username))
          case _ => None
        },
        { x: Option[PreviousUsername] => Option((x.map(_.id), x.map(_.createdAt), x.map(_.userId), x.map(_.username))) }
      )
  }
}

object ZuulTables {
  def apply[Profile <: JdbcProfile](_driver: Profile) = new ZuulTables[Profile] {
    override val driver = _driver
  }
}
