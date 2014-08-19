package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.ZuulAuthorizationErrorCode.InvalidToken
import com.blinkbox.books.auth.server.ZuulAuthorizationErrorReason.UnverifiedIdentity
import com.blinkbox.books.auth.server.env.{CommonResponder, TestEnv, TokenStatusEnv}
import com.blinkbox.books.auth.server.sso.{SSOTokenElevation, SSOTokenStatus}
import com.blinkbox.books.auth.server.{RefreshTokenStatus, SessionInfo, ZuulAuthorizationException}
import com.blinkbox.books.auth.{Elevation, User}
import spray.http.StatusCodes

class DefaultSessionServiceSpecs extends SpecBase {

  implicit val user = User(Map(
    "sub" -> "urn:blinkbox:zuul:user:1",
    "zl/rti" -> Int.box(1),
    "sso/at" -> "some-access-token"
  ))

  "The session service" should "report the status of a refresh token bound to a critically elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.Critical)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Critical), Some(300), None) =>
    })
  }

  it should "report the status of a refresh token bound to an un-elevated SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Valid, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

  it should "report the status of a refresh token bound to a revoked SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Revoked, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of a refresh token bound to an expired SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Expired, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "report the status of a refresh token bound to an invalid SSO token" in new TokenStatusEnv {
    ssoSessionInfo(SSOTokenStatus.Invalid, SSOTokenElevation.None)

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Invalid, None, None, None) =>
    })
  }

  it should "use zuul information to provide status of a refresh token that is not bound to any SSO token" in new TokenStatusEnv {
    ssoNoInvocation()
    removeSSOTokens()

    whenReady(sessionService.querySession())(_ should matchPattern {
      case SessionInfo(RefreshTokenStatus.Valid, Some(Elevation.Unelevated), None, None) =>
    })
  }

  it should "extend an user session by invoking the SSO service" in new TestEnv with CommonResponder {
    ssoNoContent()

    whenReady(sessionService.extendSession()) { _ => }
  }

  it should "not extend an user session if the SSO refresh token is not available" in new TestEnv with CommonResponder {
    ssoNoInvocation()
    removeSSOTokens()

    failingWith[ZuulAuthorizationException](sessionService.extendSession()) should matchPattern {
      case ZuulAuthorizationException(_, InvalidToken, Some(UnverifiedIdentity)) =>
    }
  }

  it should "not extend an user session if the authenticated user doesn't have an SSO token" in new TestEnv with CommonResponder {
    ssoNoInvocation()

    val u = user.copy(claims = user.claims - "sso/at")

    failingWith[ZuulAuthorizationException](sessionService.extendSession()(u)) should matchPattern {
      case ZuulAuthorizationException(_, InvalidToken, Some(UnverifiedIdentity)) =>
    }
  }

  it should "signal that the SSO service didn't recognize the provided credentials when extending a session" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[ZuulAuthorizationException](sessionService.extendSession()) should matchPattern {
      case ZuulAuthorizationException(_, InvalidToken, Some(UnverifiedIdentity)) =>
    }
  }
}
