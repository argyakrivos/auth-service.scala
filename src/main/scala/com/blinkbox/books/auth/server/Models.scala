package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.Elevation.Elevation
import com.blinkbox.books.auth.server.ZuulRequestErrorCode.InvalidClient
import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field
import org.joda.time.DateTime

object RefreshTokenStatus extends Enumeration {
  type RefreshTokenStatus = Value
  val Valid = Value("VALID")
  val Invalid = Value("INVALID")
}

import RefreshTokenStatus._

case class UserRegistration(
  firstName: String,
  lastName: String,
  username: String,
  password: String,
  acceptedTerms: Boolean,
  allowMarketing: Boolean,
  clientName: Option[String],
  clientBrand: Option[String],
  clientModel: Option[String],
  clientOS: Option[String]) {
  val client = {
    val details = Seq(clientName, clientBrand, clientModel, clientOS).flatten

    require(details.size == 0 || details.size == 4, "Incomplete client details")

    details.size match {
      case 4 => Some(ClientRegistration(details(0), details(1), details(2), details(3)))
      case 0 => None
    }
  }
}

trait ClientCredentials {
  val clientId: Option[String]
  val clientSecret: Option[String]
}

case class PasswordCredentials(username: String, password: String, clientId: Option[String], clientSecret: Option[String]) extends ClientCredentials {
  if (clientId.isDefined ^ clientSecret.isDefined) FailWith.requestException("Both client id and client secret are required.", InvalidClient)
}

case class RefreshTokenCredentials(token: String, clientId: Option[String], clientSecret: Option[String]) extends ClientCredentials {
  if (clientId.isDefined ^ clientSecret.isDefined) FailWith.requestException("Both client id and client secret are required.", InvalidClient)
}

case class ClientRegistration(
  name: String,
  brand: String,
  model: String,
  os: String) {

  require(!name.isEmpty, "Name must not be empty if provided")
  require(!brand.isEmpty, "Brand must not be empty if provided")
  require(!model.isEmpty, "Model must not be empty if provided")
  require(!os.isEmpty, "OS must not be empty if provided")
}

case class TokenInfo(
  access_token: String,
  token_type: String,
  expires_in: Int,
  refresh_token: Option[String],
  user_id: String,
  user_uri: String,
  user_username: String,
  user_first_name: String,
  user_last_name: String,
  client_id: Option[String] = None,
  client_uri: Option[String] = None,
  client_name: Option[String] = None,
  client_brand: Option[String] = None,
  client_model: Option[String] = None,
  client_os: Option[String] = None,
  client_secret: Option[String] = None,
  last_used_date: Option[DateTime] = None)

case class ClientInfo(
  client_id: String,
  client_uri: String,
  client_name: String,
  client_brand: String,
  client_model: String,
  client_os: String,
  client_secret: Option[String],
  last_used_date: DateTime)

case class ClientPatch(
  client_name: Option[String] = None,
  client_brand: Option[String] = None,
  client_model: Option[String] = None,
  client_os: Option[String] = None)

case class ClientList(clients: List[ClientInfo])

case class SessionInfo(
  token_status: RefreshTokenStatus,
  token_elevation: Option[Elevation],
  token_elevation_expires_in: Option[Long],
  user_roles: Option[List[String]] = None)

// TODO: Add user patch properties
@ApiModel(description = "Updates to a user")
case class UserPatch(
  @(ApiModelProperty @field)(position = 0, value = "The name") name: String) {
  require(name.length > 0)
}