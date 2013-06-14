
Given(/^I have registered a client$/) do
  provide_access_token
  submit_client_registration_request
  check_client_information_response
end

Given(/^I have (not )?provided my client access token$/) do |no_token|
  @request_headers ||= {}
  if no_token
    @request_headers.delete("Authorization")
  else
    @request_headers["Authorization"] = "Bearer #{@client_response["registration_access_token"]}"
  end
end

Given(/^I have (not )?provided a client name$/) do |no_name|
  @client_info ||= {}
  @client_info["client_name"] = "My Test Client" unless no_name
end

Given(/^the client details I have provided are malformed$/) do
  @client_info = "this doesn't parse as json!"
end

Given(/^I have provided the access token for a different client$/) do
  old_registration_access_token = @client_response["registration_access_token"]
  provide_access_token
  submit_client_registration_request
  @request_headers["Authorization"] = "Bearer #{old_registration_access_token}"
  check_client_information_response
end

When(/^I submit the client registration request$/) do
  submit_client_registration_request
end

When(/^I submit the client information request$/) do
  begin
    @response = @agent.request_with_entity(:get, @client_response["registration_client_uri"], "", @request_headers)
    # p @response.body
  rescue Mechanize::ResponseCodeError => e
    @response = e.page
    # p e.page.body
  end
end

Then(/^the response contains client information, including a client secret$/) do
  check_client_information_response
end

Then(/^the client name should match the provided name$/) do
  @client_response["client_name"].should == @client_info["client_name"]
end

Then(/^a client name should have been created for me$/) do
  @client_response["client_name"].should_not be nil
end