package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.server.env.{CommonResponder, TestEnv}
import com.blinkbox.books.testkit.FailHelper
import org.scalatest.{FlatSpec, Matchers}
import spray.http.StatusCodes

class UpdatePasswordSpecs extends FlatSpec with Matchers with SpecBase with FailHelper {

  "The SSO client" should "return a successful future if the SSO service replies with a 204" in new TestEnv with CommonResponder {
    ssoNoContent()

    whenReady(sso.updatePassword(SSOAccessToken("some-access-token"), "an-old-password", "a-new-password")) { _ => }
  }

  it should "signal a request failure if the SSO service replies with a 400" in new TestEnv with CommonResponder {
    ssoInvalidRequest("Some error")

    failingWith[SSOInvalidRequest](sso.updatePassword(SSOAccessToken("some-access-token"), "an-old-password", "a-new-password")) should matchPattern {
      case SSOInvalidRequest("Some error") =>
    }
  }

  it should "signal an authentication failure in the SSO service" in new TestEnv with CommonResponder {
    ssoResponse(StatusCodes.Unauthorized)

    failingWith[SSOUnauthorized.type](sso.updatePassword(SSOAccessToken("some-access-token"), "an-old-password", "a-new-password"))
  }

}