package com.blinkbox.books.auth.server

import scala.concurrent.duration.FiniteDuration

sealed trait ZuulErrorCode
sealed trait ZuulRequestErrorCode extends ZuulErrorCode
sealed trait ZuulRequestErrorReason extends ZuulErrorCode

sealed trait ZuulErrorReason
sealed trait ZuulAuthorizationErrorCode extends ZuulErrorReason
sealed trait ZuulAuthorizationErrorReason extends ZuulErrorReason

trait EnumContainer[T] {
  val reprs: Map[T, String]

  lazy val inverseReprs = reprs.map(_.swap).toMap

  def fromString(s: String): T = inverseReprs(s)
  def toString(c: T): String = reprs(c)
}

object ZuulRequestErrorCode extends EnumContainer[ZuulRequestErrorCode] {
  case object InvalidClient extends ZuulRequestErrorCode
  case object InvalidGrant extends ZuulRequestErrorCode
  case object InvalidRequest extends ZuulRequestErrorCode

  val reprs: Map[ZuulRequestErrorCode, String] = Map(
    InvalidClient -> "invalid_client",
    InvalidGrant -> "invalid_grant",
    InvalidRequest -> "invalid_request"
  )
}

object ZuulRequestErrorReason extends EnumContainer[ZuulRequestErrorReason] {
  case object CountryGeoBlocked extends ZuulRequestErrorReason
  case object UsernameAlreadyTaken extends ZuulRequestErrorReason
  case object ClientLimitReached extends ZuulRequestErrorReason
  case object OldPasswordInvalid extends ZuulRequestErrorReason
  case object NewPasswordTooShort extends ZuulRequestErrorReason
  case object NewPasswordMissing extends ZuulRequestErrorReason

  val reprs: Map[ZuulRequestErrorReason, String] = Map(
    CountryGeoBlocked -> "country_geoblocked",
    UsernameAlreadyTaken -> "username_already_taken",
    ClientLimitReached -> "client_limit_reached",
    OldPasswordInvalid -> "old_password_invalid",
    NewPasswordTooShort -> "new_password_too_short",
    NewPasswordMissing -> "new_password_missing"
  )
}

object ZuulAuthorizationErrorCode extends EnumContainer[ZuulAuthorizationErrorCode] {
  case object InvalidToken extends ZuulAuthorizationErrorCode

  val reprs: Map[ZuulAuthorizationErrorCode, String] = Map(InvalidToken -> "invalid_token")
}

object ZuulAuthorizationErrorReason extends EnumContainer[ZuulAuthorizationErrorReason] {
  case object UnverifiedIdentity extends ZuulAuthorizationErrorReason

  val reprs: Map[ZuulAuthorizationErrorReason, String] = Map(UnverifiedIdentity -> "unverified_identity")
}

sealed trait ZuulException extends Throwable {
  def message: String
}

case class ZuulRequestException(
  message: String, code: ZuulRequestErrorCode, reason: Option[ZuulRequestErrorReason] = None) extends ZuulException

case class ZuulAuthorizationException(
  message: String, code: ZuulAuthorizationErrorCode, reason: Option[ZuulAuthorizationErrorReason] = None) extends ZuulException

case class ZuulTooManyRequestException(message: String, retryAfter: FiniteDuration) extends ZuulException

case class ZuulUnknownException(message: String, ex: Option[Throwable] = None) extends ZuulException

object Failures {
  import com.blinkbox.books.auth.server.ZuulAuthorizationErrorCode._
  import com.blinkbox.books.auth.server.ZuulAuthorizationErrorReason._
  import com.blinkbox.books.auth.server.ZuulRequestErrorCode._
  import com.blinkbox.books.auth.server.ZuulRequestErrorReason._

  def usernameAlreadyTaken = ZuulRequestException("The selected username is already taken", InvalidRequest, Some(UsernameAlreadyTaken))
  def invalidPasswordResetToken = ZuulRequestException("The password-reset token is invalid.", InvalidGrant)
  def invalidRefreshToken = ZuulRequestException("The refresh token is invalid.", InvalidGrant)
  def refreshTokenNotAuthorized = ZuulRequestException("Your client is not authorised to use this refresh token", InvalidClient)
  def unverifiedIdentity = ZuulAuthorizationException("User identity must be reverified", InvalidToken, Some(UnverifiedIdentity))
  def termsAndConditionsNotAccepted = ZuulRequestException("You must accept the terms and conditions", InvalidRequest)
  def newPasswordTooShort = ZuulRequestException("Password must be at least 6 characters", InvalidRequest, Some(NewPasswordTooShort))
  def newPasswordMissing = ZuulRequestException("The new password is not provided", InvalidRequest, Some(NewPasswordMissing))
  def notInTheUK = ZuulRequestException("You must be in the UK to register", InvalidRequest, Some(CountryGeoBlocked))
  def invalidUsernamePassword = ZuulRequestException("The username and/or password is incorrect.", InvalidGrant)
  def invalidClientCredentials = ZuulRequestException("Invalid client credentials.", InvalidClient)
  def clientLimitReached = ZuulRequestException("Max clients ($MaxClients) already registered", InvalidRequest, Some(ClientLimitReached))
  def oldPasswordInvalid = ZuulRequestException("Old password is invalid", InvalidRequest, Some(OldPasswordInvalid))

  def passwordResetTokenNotValid = ZuulRequestException("The password reset token is invalid", InvalidRequest)
  def passwordResetTokenExpired = ZuulRequestException("The password reset token has expired", InvalidRequest)
  def passwordResetTokenRevoked = ZuulRequestException("The password reset token has been revoked", InvalidRequest)

  def unknownError(msg: String, ex: Option[Throwable] = None) = ZuulUnknownException(msg, ex)

  def tooManyRequests(retryAfter: FiniteDuration) =
    ZuulTooManyRequestException(s"Too many login attempts, please retry after ${retryAfter.toSeconds} seconds", retryAfter)

  def requestException(message: String, code: ZuulRequestErrorCode, reason: Option[ZuulRequestErrorReason] = None) =
    ZuulRequestException(message, code, reason)
}
