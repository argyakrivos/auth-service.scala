Given(/^I am authenticated as a user in the "(.*?)" role$/) do |role_name|
  # yes, this is a bit crap, but until we have role management it's about as good as we can do...
  @me = TestUser.new
  @me.username = "tm-books-itops@blinkbox.com"
  @me.password = "d41P8YETV7OjU^cufcu0"
  obtain_access_and_token_via_username_and_password
end

Given(/^there is a registered user, call (?:him|her|them) "(.+)"$/) do |user_handle|
  register_random_user(user_handle)
end

Given(/^there is a registered user, call (?:him|her|them) "(.+)", who has previously changed (?:his|her|their) email address$/) do |user_handle|
  user = register_random_user(user_handle)
  user.username = random_email
  $zuul.update_user(user, user.access_token)
end

When(/^I search for users with (\w+)'s email address$/) do |user_handle|
  user = @known_users[user_handle]
  raise "Unknown user '#{user_handle}'" unless user
  $zuul.admin_find_user({ username: user.username }, @me.access_token)
end

When(/^I search for users with (\w+)'s first name and last name$/) do |user_handle|
  user = @known_users[user_handle]
  raise "Unknown user '#{user_handle}'" unless user
  $zuul.admin_find_user({ first_name: user.first_name, last_name: user.last_name }, @me.access_token)
end

When(/^I search for users with (\w+)'s user id$/) do |user_handle|
  user = @known_users[user_handle]
  raise "Unknown user '#{user_handle}'" unless user
  $zuul.admin_find_user({ user_id: user.id }, @me.access_token)
end

Then(/^the response is a list containing (#{CAPTURE_INTEGER}) users?$/) do |count|
  expect(last_response.status).to eq(200)
  list = last_response_json
  expect(list["items"]).to be_a_kind_of(Array)
  expect(list["items"]).to have(count).users
end

Then(/^the (#{CAPTURE_NAMED_INDEX}) user matches (\w+)'s attributes$/) do |index, user_handle|
  expected_user = @known_users[user_handle]
  raise "Unknown user '#{user_handle}'" unless expected_user
  actual_user = last_response_json["items"][index]
  expect(actual_user["user_username"]).to eq(expected_user.username)
  expect(actual_user["user_first_name"]).to eq(expected_user.first_name)
  expect(actual_user["user_last_name"]).to eq(expected_user.last_name)
  expect(actual_user["user_allow_marketing_communications"]).to eq(expected_user.allow_marketing_communications)
end

def register_random_user(handle)
  user = TestUser.new.generate_details
  user.register
  Cucumber::Rest::Status.ensure_status_class(:success)

  @known_users ||= {}
  @known_users[handle] = user

  user
end