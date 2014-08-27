package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.{UserPatch, PasswordCredentials, SSOConfig, UserRegistration}
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.json4s.JsonAST.{JField, JObject, JString}
import spray.client.pipelining._
import spray.http.{FormData, HttpCredentials, OAuth2BearerToken, StatusCodes}
import spray.httpx.UnsuccessfulResponseException

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

sealed trait SSOException extends Throwable
case class SSOInvalidAccessToken(receivedCredentials: SSOCredentials) extends SSOException
case object SSOUnauthorized extends SSOException
case object SSOConflict extends SSOException
case object SSOForbidden extends SSOException
case class SSOTooManyRequests(retryAfter: FiniteDuration) extends SSOException
case class SSOInvalidRequest(message: String) extends SSOException
case object SSONotFound extends SSOException
case class SSOUnknownException(e: Throwable) extends SSOException

object SSOConstants {
  val TokenUri = "/oauth2/token"
  val LinkUri = "/link"
  val UserInfoUri = "/user"
  val RevokeTokenUri = "/tokens/revoke"
  val TokenStatusUri = "/tokens/status"
  val ExtendSessionUri = "/session"
  val UpdatePasswordUri = "/password/update"
  val GeneratePasswordResetTokenUri = "/password/reset/generate-token"

  val RegistrationGrant = "urn:blinkbox:oauth:grant-type:registration"
  val PasswordResetTokenGrant = "urn:blinkbox:oauth:grant-type:password-reset-token"
  val PasswordGrant = "password"
  val RefreshTokenGrant = "refresh_token"
}

trait SSO {
  def register(req: UserRegistration): Future[(SSOUserId, SSOCredentials)]
  def authenticate(c: PasswordCredentials): Future[SSOCredentials]
  def refresh(ssoRefreshToken: String): Future[SSOCredentials]
  def resetPassword(passwordToken: String, newPassword: String): Future[SSOUserCredentials]
  def revokeToken(ssoRefreshToken: String): Future[Unit]
  // // User - authenticated
  def linkAccount(token: SSOAccessToken, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit]
  def generatePasswordResetToken(username: String): Future[SSOPasswordResetToken]
  def updatePassword(token: SSOAccessToken, oldPassword: String, newPassword: String): Future[Unit]
  def sessionStatus(token: SSOAccessToken): Future[TokenStatus]
  def extendSession(token: SSOAccessToken): Future[Unit]
  def userInfo(token: SSOAccessToken): Future[UserInformation]
  def updateUser(token: SSOAccessToken, req: UserPatch): Future[Unit]
  // // Admin
  // def adminSearchUser(req: SearchUser): Future[SearchUserResult]
  // def adminUserDetails(req: GetUserDetails): Future[UserDetail]
  // def adminUpdateUser(req: UpdateUser): Future[UserDetail]
  // // Health-check
  // def systemStatus(): Future[SystemStatus]
}

class DefaultSSO(config: SSOConfig, client: Client, tokenDecoder: SsoAccessTokenDecoder)(implicit ec: ExecutionContext) extends SSO with StrictLogging {
  import com.blinkbox.books.auth.server.sso.Serialization.json4sUnmarshaller

  private def versioned(uri: String) = s"/${config.version}$uri"

  private val C = SSOConstants

  private def extractUserId(cred: SSOCredentials): (SSOUserId, SSOCredentials) = SsoDecodedAccessToken.decode(cred.accessToken.value, tokenDecoder) match {
    case Success(token) => (SSOUserId(token.subject), cred)
    case Failure(_) => throw new SSOInvalidAccessToken(cred)
  }

  private def userCredentials(cred: SSOCredentials): SSOUserCredentials = {
    val (id, _) = extractUserId(cred)
    SSOUserCredentials(id, cred)
  }

  private def extractInvalidRequest(e: UnsuccessfulResponseException): SSOException = {
    import org.json4s.jackson.JsonMethods._
    parseOpt(e.response.entity.asString).collect {
      case JObject(
        JField("error", JString("invalid_request")) ::
        JField("error_description", JString(s)) :: Nil) => SSOInvalidRequest(s)
    } getOrElse(SSOUnknownException(e))
  }

  private def extractTooManyRequests(e: UnsuccessfulResponseException): SSOException = try {
    import scala.concurrent.duration._
    e.response.
      headers.
      find(_.lowercaseName == "retry-after").
      map(r => SSOTooManyRequests(r.value.toInt.seconds)).
      getOrElse(SSOUnknownException(e))
  } catch {
    case e: NumberFormatException => SSOUnknownException(e)
  }

  private def commonErrorsTransformer: Throwable => SSOException = {
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Unauthorized => SSOUnauthorized
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Forbidden => SSOForbidden
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.Conflict => SSOConflict
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.BadRequest => extractInvalidRequest(e)
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.TooManyRequests => extractTooManyRequests(e)
    case e: UnsuccessfulResponseException if e.response.status == StatusCodes.NotFound => SSONotFound
    case e: Throwable => SSOUnknownException(e)
  }

  // TODO: These transformers should deal with some specific exception and then forward to common for unhandled ones
  private def registrationErrorsTransformer = commonErrorsTransformer
  private def authenticationErrorsTransformer = commonErrorsTransformer
  private def linkErrorsTransformer = commonErrorsTransformer
  private def refreshErrorsTransformer = commonErrorsTransformer
  private def userInfoErrorsTransformer = commonErrorsTransformer
  private def revokeTokenErrorsTransformer = commonErrorsTransformer
  private def tokenStatusErrorsTransformer = commonErrorsTransformer
  private def extendSessionErrorTransformer = commonErrorsTransformer
  private def updateUserErrorTransformer = commonErrorsTransformer
  private def updatePasswordErrorTransformer = commonErrorsTransformer
  private def generatePasswordTokenErrorTransformer = commonErrorsTransformer
  private def resetPasswordErrorTransformer = commonErrorsTransformer

  def oauthCredentials(token: SSOAccessToken): HttpCredentials = new OAuth2BearerToken(token.value)

  def register(req: UserRegistration): Future[(SSOUserId, SSOCredentials)] = {
    logger.debug("Registering user")
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RegistrationGrant,
      "first_name" -> req.firstName,
      "last_name" -> req.lastName,
      "username" -> req.username,
      "password" -> req.password
    )))) map extractUserId transform(identity, registrationErrorsTransformer)
  }

  def linkAccount(token: SSOAccessToken, id: UserId, allowMarketing: Boolean, termsVersion: String): Future[Unit] = {
    logger.debug("Linking account", id)
    client.unitRequest(Post(versioned(C.LinkUri), FormData(Map(
      "service_user_id" -> id.external,
      "service_allow_marketing" -> allowMarketing.toString,
      "service_tc_accepted_version" -> termsVersion
    ))), oauthCredentials(token)) transform(identity, linkErrorsTransformer)
  }

  def authenticate(c: PasswordCredentials): Future[SSOCredentials] = {
    logger.debug("Authenticating via password credentials")
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.PasswordGrant,
      "username" -> c.username,
      "password" -> c.password
    )))) map extractUserId transform(_._2, authenticationErrorsTransformer)
  }

  def userInfo(token: SSOAccessToken): Future[UserInformation] = {
    logger.debug("Fetching user info")
    client.dataRequest[UserInformation](Get(versioned(C.UserInfoUri)), oauthCredentials(token)) transform(identity, userInfoErrorsTransformer)
  }

  def refresh(ssoRefreshToken: String): Future[SSOCredentials] = {
    logger.debug("Authenticating via refresh token")
    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.RefreshTokenGrant,
      "refresh_token" -> ssoRefreshToken
    )))) transform(identity, refreshErrorsTransformer)
  }

  def revokeToken(ssoRefreshToken: String): Future[Unit] = {
    logger.debug("Revoking refresh token")
    client.unitRequest(Post(versioned(C.RevokeTokenUri), FormData(Map(
      "token" -> ssoRefreshToken
    )))) transform(identity, revokeTokenErrorsTransformer)
  }

  def sessionStatus(token: SSOAccessToken): Future[TokenStatus] = {
    logger.debug("Fetching token status")
    client.dataRequest[TokenStatus](Post(versioned(C.TokenStatusUri), FormData(Map(
      "token" -> token.value
    )))) transform(identity, tokenStatusErrorsTransformer)
  }

  def extendSession(token: SSOAccessToken): Future[Unit] = {
    logger.debug("Refreshing session")
    client.unitRequest(
      Post(versioned(C.ExtendSessionUri), FormData(Map.empty[String, String])),
      oauthCredentials(token)) transform(identity, extendSessionErrorTransformer)
  }

  def updateUser(token: SSOAccessToken, req: UserPatch): Future[Unit] = {
    logger.debug("Updating user")

    val formData = Seq(
      req.username.map("username" -> _),
      req.first_name.map("first_name" -> _),
      req.last_name.map("last_name" -> _),
      req.allow_marketing_communications.map("service_allow_marketing" -> _.toString)
    ).flatten

    client.unitRequest(
      Patch(versioned(C.UserInfoUri), FormData(formData)), oauthCredentials(token)) transform(identity, updateUserErrorTransformer)
  }

  def updatePassword(token: SSOAccessToken, oldPassword: String, newPassword: String): Future[Unit] = {
    logger.debug("Changing password")

    client.unitRequest(Post(versioned(C.UpdatePasswordUri), FormData(Map(
      "old_password" -> oldPassword,
      "new_password" -> newPassword
    ))), oauthCredentials(token)) transform(identity, updatePasswordErrorTransformer)
  }

  def generatePasswordResetToken(username: String): Future[SSOPasswordResetToken] = {
    logger.debug("Generate password-reset token")

    client.dataRequest[SSOPasswordResetToken](Post(versioned(C.GeneratePasswordResetTokenUri), FormData(Map(
      "username" -> username
    )))) transform(identity, generatePasswordTokenErrorTransformer)
  }

  def resetPassword(passwordToken: String, newPassword: String): Future[SSOUserCredentials] = {
    logger.debug("Reset password")

    client.dataRequest[SSOCredentials](Post(versioned(C.TokenUri), FormData(Map(
      "grant_type" -> C.PasswordResetTokenGrant,
      "password_reset_token" -> passwordToken,
      "password" -> newPassword
    )))) map(userCredentials) transform(identity, resetPasswordErrorTransformer)
  }
}
