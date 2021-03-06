package com.blinkbox.books.auth.server.data

import java.util.concurrent.TimeUnit

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.UserRole.UserRole
import com.blinkbox.books.auth.server.TokenStatus
import com.blinkbox.books.auth.server.sso.{SsoRefreshToken, SsoUserId}
import com.blinkbox.books.time.Clock
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

case class UserId(value: Int) extends AnyVal {
  def external = s"urn:blinkbox:zuul:user:$value"
  def uri = s"/users/$value"
}

object UserId { val Invalid = UserId(-1) }

object ExternalUserId {
  val expr = """urn:blinkbox:zuul:user:(\d+)""".r
  def unapply(idString: String): Option[UserId] = idString match {
    case expr(id) => Some(UserId(id.toInt))
    case _ => None
  }
}

case class ClientId(value: Int) extends AnyVal {
  def external = s"urn:blinkbox:zuul:client:$value"
  def uri = s"/clients/$value"
}

object ClientId { val Invalid = ClientId(-1) }

object ExternalClientId {
  val expr = """urn:blinkbox:zuul:client:(\d+)""".r
  def unapply(idString: String): Option[ClientId] = idString match {
    case expr(id) => Some(ClientId(id.toInt))
    case _ => None
  }
}

case class RefreshTokenId(value: Int) extends AnyVal

object RefreshTokenId { val Invalid = RefreshTokenId(-1) }

case class RoleId(value: Int) extends AnyVal

case class Role(id: RoleId, name: UserRole, description: String)

case class PrivilegeId(value: Int) extends AnyVal

case class Privilege(id: PrivilegeId, createdAt: DateTime, userId: UserId, roleId: RoleId)

case class User(id: UserId,
    createdAt: DateTime,
    updatedAt: DateTime,
    username: String,
    firstName: String,
    lastName: String,
    passwordHash: String,
    allowMarketing: Boolean,
    ssoId: Option[SsoUserId] = None)

case class Client(id: ClientId, createdAt: DateTime, updatedAt: DateTime, userId: UserId, name: String, brand: String, model: String, os: String, secret: String, isDeregistered: Boolean)

case class RefreshToken(
    id: RefreshTokenId,
    createdAt: DateTime,
    updatedAt: DateTime,
    userId: UserId,
    clientId: Option[ClientId],
    token: String,
    ssoRefreshToken: Option[SsoRefreshToken],
    isRevoked: Boolean,
    expiresAt: DateTime,
    elevationExpiresAt: DateTime,
    criticalElevationExpiresAt: DateTime) {
  def isExpired(implicit clock: Clock) = expiresAt.isBefore(clock.now())
  def isValid(implicit clock: Clock) = !isExpired && !isRevoked
  def status(implicit clock: Clock) = if (isValid) TokenStatus.Valid else TokenStatus.Invalid
  def isElevated(implicit clock: Clock) = !elevationExpiresAt.isBefore(clock.now())
  def isCriticallyElevated(implicit clock: Clock) = !criticalElevationExpiresAt.isBefore(clock.now())
  def elevation(implicit clock: Clock) =
    if (ssoRefreshToken.isEmpty) Elevation.Unelevated
    else if (isCriticallyElevated) Elevation.Critical
    else if (isElevated) Elevation.Elevated
    else Elevation.Unelevated
  def elevationDropsAt(implicit clock: Clock) = if (isCriticallyElevated) criticalElevationExpiresAt else elevationExpiresAt
  def elevationDropsIn(implicit clock: Clock) = FiniteDuration(elevationDropsAt.getMillis - clock.now().getMillis, TimeUnit.MILLISECONDS)
}

case class LoginAttempt(createdAt: DateTime, username: String, successful: Boolean, clientIP: String)

case class PreviousUsernameId(value: Int) extends AnyVal

object PreviousUsernameId {
  val invalid: PreviousUsernameId = PreviousUsernameId(-1)
}

case class PreviousUsername(id: PreviousUsernameId, createdAt: DateTime, userId: UserId, username: String)
