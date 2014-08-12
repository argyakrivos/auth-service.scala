package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.data.UserId

// sealed trait Token
// case class RefreshToken() extends Token
// case class PasswordResetToken() extends Token
// case class AccessToken() extends Token

//case class RegisterUser(
//  id: UserId,
//  firstName: String,
//  lastName: String,
//  username: String,
//  password: String,
//  acceptedTermsVersion: String,
//  allowMarketing: Boolean) extends Request

// case class AuthenticateUser() extends Request
// case class RevokeToken(token: Token)
// case class LinkAccount() extends Request
// case class GeneratePasswordReset() extends Request
// case class UpdatePassword() extends Request
// case class GetTokenStatus() extends Request
// case class PatchUser() extends Request

// case class SearchUser() extends AdminRequest
// case class GetUserDetails() extends AdminRequest
// case class UpdateUser() extends AdminRequest

case class SSOCredentials(accessToken: String, tokenType: String, expiresIn: Int, refreshToken: String) {
  require(tokenType.toLowerCase == "bearer", s"Unrecognized token type: $tokenType")
}

case class LinkedAccount(service: String, serviceUserId: String, serviceAllowMarketing: Boolean)

case class UserInformation(userId: String, username: String, firstName: String, lastName: String, linkedAccounts: List[LinkedAccount])

// case class PasswordResetCredentials() extends Response
// case class TokenStatus() extends Response
// case class UserInformation() extends Response
// case class SystemStatus() extends Response

// case class SearchUserResult() extends AdminResponse
// case class UserDetail() extends AdminResponse
