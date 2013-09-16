Given(/^I have deregistered my current client$/) do
  $zuul.deregister_client(@my_client.local_id.to_i, @me.access_token)
end

When(/^I request that my (current|other) client be deregistered(, without my access token)?$/) do |client, without_token|
  requested_client = client == "current" ? @my_client : @my_other_client
  access_token = without_token.nil? ? @me.access_token : nil
  $zuul.deregister_client(requested_client.local_id.to_i, access_token)
end

When(/^I request that (the other user's|a nonexistent) client be deregistered$/) do |client|
  client_id = client == "the other user's" ? @your_client.id.to_i : @my_client.local_id.to_i + 1000
  $zuul.deregister_client(client_id, @me.access_token)
end

Then(/^my refresh token and access token are (valid|invalid because they have been revoked)$/) do |validity|
  step("I request information about the access token")
  step("the response contains access token information")
  if validity == "valid"
    expect(last_response_json['token_status']).to eql('VALID')
  else
    step("the request fails because I am unauthorised")
    @response_json = MultiJson.load(last_response.body)
    expect(@response_json["error"]).to eq("invalid_request")
    authenticate_header = Hash[*last_response['WWW-Authenticate'].scan(/([^\ ]+)="([^\"]+)"/).flatten]
    expect(authenticate_header["error_description"]).to eq("invalid_token")
  end
end

Then(/^I have got #{CAPTURE_INTEGER} registered clients?$/) do |num|
  expect(@clients.size).to eq num
end



