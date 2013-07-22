class CreateUsers < ActiveRecord::Migration

  def self.up
    create_table :users do |t|
      t.timestamps
      t.string :first_name, limit: 50
      t.string :last_name, limit: 50
      t.string :email, limit: 320
      t.string :password_hash, limit: 128
      t.boolean :allow_marketing_communications
    end
    add_index :users, :email, unique: true
  end

  def self.down
    remove_index :users, :email
    drop_table :users
  end

end