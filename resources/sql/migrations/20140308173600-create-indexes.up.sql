CREATE INDEX idx_user_account_created
ON user_account (created ASC);
--;;
CREATE INDEX idx_user_account_last_login
ON user_account (last_login ASC);
--;;
CREATE INDEX idx_log_created
ON log (created ASC);
