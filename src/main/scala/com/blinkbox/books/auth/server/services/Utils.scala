package com.blinkbox.books.auth.server.services

import com.blinkbox.books.auth.server.data.{UserId, AuthRepository, Client, User}
import com.blinkbox.books.auth.server.{ClientInfo, UserInfo, ClientCredentials, Failures}

import scala.slick.profile.BasicProfile

trait ClientAuthenticator[Profile <: BasicProfile] {
  protected def authenticateClient(
      authRepo: AuthRepository[Profile],
      credentials: ClientCredentials,
      userId: UserId)(implicit session: authRepo.Session): Option[Client] =
    for {
      clientId <- credentials.clientId
      clientSecret <- credentials.clientSecret
    } yield authRepo.
      authenticateClient(clientId, clientSecret, userId).
      getOrElse(throw Failures.invalidClientCredentials)
}

trait UserInfoFactory {
  def userInfoFromUser(user: User) = UserInfo(
    user_id = user.id.external,
    user_uri = s"/users/${user.id.value}",
    user_username = user.username,
    user_first_name = user.firstName,
    user_last_name = user.lastName,
    user_allow_marketing_communications = user.allowMarketing
  )
}

trait ClientInfoFactory {
  def clientInfo(client: Client, includeClientSecret: Boolean = false) = ClientInfo(
    client_id = client.id.external,
    client_uri = s"/clients/${client.id.value}",
    client_name = client.name,
    client_brand = client.brand,
    client_model = client.model,
    client_os = client.os,
    client_secret = if (includeClientSecret) Some(client.secret) else None,
    last_used_date = client.createdAt)
}