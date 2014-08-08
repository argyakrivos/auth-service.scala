package com.blinkbox.books.auth.server.env

import java.util.Date

import com.blinkbox.books.auth.server._
import com.blinkbox.books.auth.server.cake._
import com.blinkbox.books.auth.server.data._
import com.blinkbox.books.auth.server.sso.{SsoAccessTokenDecoder, DefaultSSO, SSOResponseMocker, TestSSOClient}
import com.blinkbox.books.auth.{User => AuthenticatedUser}
import com.blinkbox.books.slick.DBTypes
import com.blinkbox.books.testkit.{PublisherSpy, TestH2}
import com.blinkbox.books.time.{StoppedClock, TimeSupport}
import org.joda.time.Duration

import scala.reflect.ClassTag
import scala.slick.driver.{H2Driver, JdbcProfile}

trait StoppedClockSupport extends TimeSupport {
  override val clock = StoppedClock()
}

class TestDBTypes extends DBTypes {
  type Profile = JdbcProfile
  type ConstraintException = org.h2.jdbc.JdbcSQLException
  val constraintExceptionTag = implicitly[ClassTag[ConstraintException]]
}

trait TestDatabaseComponent extends DatabaseComponent {
  val Types = new TestDBTypes

  override val db = TestH2.db
  override val driver = H2Driver
  override val tables = ZuulTables[Types.Profile](driver)
}

trait TestEventsComponent extends EventsComponent {
  val publisherSpy = new PublisherSpy
  override val publisher = publisherSpy
}

trait TestPasswordHasherComponent extends PasswordHasherComponent {
  override val passwordHasher = PasswordHasher(identity, (s1, s2) => s1 == s2)
}

trait TestSSOComponent extends SSOComponent {
  this: ConfigComponent with AsyncComponent =>

  val ssoResponse = new SSOResponseMocker

  private val client = new TestSSOClient(config.sso, ssoResponse.nextResponse)
  private val tokenDecoder = new SsoAccessTokenDecoder(SSOTestKeyStore) {
    override def validateExpirationTime(expirationTime: Date) = {} // allow expired token for test purposes
  }

  override val sso = new DefaultSSO(config.sso, client, tokenDecoder)
}

class TestEnv extends
    DefaultConfigComponent with
    DefaultAsyncComponent with
    StoppedClockSupport with
    TestSSOComponent with
    DefaultGeoIPComponent with
    TestEventsComponent with
    TestDatabaseComponent with
    TestPasswordHasherComponent with
    DefaultRepositoriesComponent with
    DefaultUserServiceComponent with
    DefaultClientServiceComponent with
    DefaultAuthServiceComponent with
    DefaultRegistrationServiceComponent with
    DefaultPasswordAuthenticationServiceComponent with
    DefaultApiComponent {

  import driver.simple._

  def now = clock.now()

  val userIdA = UserId(1)
  val userIdB = UserId(2)
  val userIdC = UserId(3)

  val userA = User(userIdA, now, now, "user.a@test.tst", "A First", "A Last", "a-password", true, Some("sso-a"))
  val userB = User(userIdB, now, now, "user.b@test.tst", "B First", "B Last", "b-password", true, Some("sso-a"))
  val userC = User(userIdC, now, now, "user.c@test.tst", "C First", "C Last", "c-password", true, Some("sso-a"))

  def fullUserPatch = UserPatch(Some("Updated First"), Some("Updated Last"), Some("updated@test.tst"), Some(false), None)

  val authenticatedUserA = AuthenticatedUser(userIdA.value, None, Map.empty)
  val authenticatedUserB = AuthenticatedUser(userIdB.value, None, Map.empty)
  val authenticatedUserC = AuthenticatedUser(userIdC.value, None, Map.empty)

  val clientIdA1 = ClientId(1)
  val clientIdA2 = ClientId(2)
  val clientIdA3 = ClientId(3)

  val clientA1 = Client(clientIdA1, now, now, userIdA, "Client A1", "Test brand A1", "Test model A1", "Test OS A1", "test-secret-a1", false)
  val clientA2 = Client(clientIdA2, now, now, userIdA, "Client A2", "Test brand A2", "Test model A2", "Test OS A2", "test-secret-a2", false)
  val clientA3 = Client(clientIdA3, now, now, userIdA, "Client A3", "Test brand A3", "Test model A3", "Test OS A3", "test-secret-a3", true)

  val exp = now.withDurationAdded(Duration.standardHours(1), 1)
  val refreshTokenClientA1 = RefreshToken(RefreshTokenId(1), now, now, userIdA, Some(clientIdA1), "some-token-a1", Some("some-sso-token-a1"), false, exp, exp, exp)
  val refreshTokenClientA2 = RefreshToken(RefreshTokenId(2), now, now, userIdA, Some(clientIdA2), "some-token-a2", Some("some-sso-token-a2"), false, exp, exp, exp)
  val refreshTokenClientA3 = RefreshToken(RefreshTokenId(3), now, now, userIdA, Some(clientIdA3), "some-token-a3", Some("some-sso-token-a3"), true, now, now, now)
  val refreshTokenNoClientA = RefreshToken(RefreshTokenId(3), now, now, userIdA, None, "some-token-a", Some("some-sso-token-a"), false, now, now, now)
  val refreshTokenNoClientDeregisteredA = RefreshToken(RefreshTokenId(3), now, now, userIdA, None, "some-token-a-deregistered", Some("some-sso-token-a-deregistered"), true, now, now, now)

  val clientsC =
    for (id <- 4 until 16)
    yield Client(ClientId(id), now, now, userIdC, s"Client C$id", s"Test brand C$id", s"Test model C$id", s"Test OS C$id", s"test-secret-c$id", false)

  val clientInfoA1 = ClientInfo(clientIdA1.external, "/clients/1", "Client A1", "Test brand A1", "Test model A1", "Test OS A1", None, now)
  val clientInfoA2 = ClientInfo(clientIdA2.external, "/clients/2", "Client A2", "Test brand A2", "Test model A2", "Test OS A2", None, now)

  val fullClientPatch = ClientPatch(Some("Patched name"), Some("Patched brand"), Some("Patched model"), Some("Patched OS"))
  val fullPatchedClientA1 = clientA1.copy(name = "Patched name", brand = "Patched brand", model = "Patched model", os = "Patched OS")
  val fullPatchedClientInfoA1 = clientInfoA1.copy(client_name = "Patched name", client_brand = "Patched brand", client_model = "Patched model", client_os = "Patched OS")

  val forbiddenClientAccesses = ((clientIdA3, authenticatedUserA) ::
    (clientIdA1, authenticatedUserB) ::
    (clientIdA2, authenticatedUserB) ::
    (clientIdA3, authenticatedUserB) ::
    (ClientId(100), authenticatedUserA) ::
    (ClientId(100), authenticatedUserB) ::Nil)

  val clientRegistration = ClientRegistration("Test name", "Test brand", "Test model", "Test OS")

  db.withSession { implicit session =>
    tables.users ++= Seq(userA, userB, userC)
    tables.clients ++= Seq(clientA1, clientA2, clientA3) ++ clientsC
    tables.refreshTokens ++= Seq(refreshTokenClientA1, refreshTokenClientA2, refreshTokenClientA3, refreshTokenNoClientA, refreshTokenNoClientDeregisteredA)
  }
}
