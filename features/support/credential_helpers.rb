
def use_username_and_password_credentials
  @credentials = {
    "grant_type" => "password",
    "username" => @user_registration_details["username"],
    "password" => @user_registration_details["password"]
  }
end

def use_refresh_token_credentials
  @credentials = {
    "grant_type" => "refresh_token",
    "refresh_token" => @user_tokens["refresh_token"]
  }
end

def include_client_credentials
  @credentials.should_not be nil
  @credentials["client_id"] = @client_response["client_id"]
  @credentials["client_secret"] = @client_response["client_secret"]
end

def submit_authentication_request
  @credentials.should_not be nil
  post_www_form_request("/oauth2/token", @credentials)
end