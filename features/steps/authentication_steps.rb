
Given(/^I have authenticated with my email address and password$/) do
  use_username_and_password_credentials
  submit_authentication_request
  validate_user_token_response
end

Given(/^I have provided my email address$/) do
  use_username_and_password_credentials
  @credentials.delete("password")
end

Given(/^I have provided my email address and password$/, :use_username_and_password_credentials)

Given(/^I have provided my email address, password and client credentials$/) do
  use_username_and_password_credentials
  include_client_credentials
end

Given(/^the email address is different from the one I used to register$/) do
  @credentials["username"] = random_email
end

Given(/^I have not provided my (password|client secret)$/) do |name|
  @credentials.delete(oauth_param_name(name))
end

Given(/^I have not provided my client credentials$/) do
  @credentials.delete("client_id")
  @credentials.delete("client_secret")
end

Given(/^I have provided an incorrect (password|client secret)$/) do |name|
  @credentials[oauth_param_name(name)] = random_password
end

Given(/^I have provided another user's client credentials$/) do
  register_new_user
  register_new_client
  include_client_credentials
end

Given(/^I have provided my refresh token?$/, :use_refresh_token_credentials) 

Given(/^I have provided my refresh token and client credentials$/) do 
  use_refresh_token_credentials
  include_client_credentials
end

Given(/^I have not provided my refresh token$/) do
  use_refresh_token_credentials
  @credentials.delete("refresh_token")
end

Given(/^I have provided an incorrect refresh token$/) do  
  use_refresh_token_credentials
  @credentials["refresh_token"] = random_password
end

Given(/^I have bound my refresh token to a client$/) do  
  register_new_client
  use_refresh_token_credentials
  include_client_credentials
  submit_authentication_request
  validate_user_token_response(refresh_token: :optional)
end

When(/^I submit the (?:authentication|access token refresh) request$/) do
  submit_authentication_request
end

Then(/^the response indicates that my (?:credentials are|refresh token is) incorrect$/) do
  @response.code.to_i.should == 400
  @response_json = MultiJson.load(@response.body)
  @response_json["error"].should == "invalid_grant"
end