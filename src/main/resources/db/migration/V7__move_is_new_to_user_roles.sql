ALTER TABLE user_roles
ADD COLUMN is_new BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE user_roles ur
SET is_new = u.is_new
FROM users u
WHERE ur.user_id = u.id;

ALTER TABLE users
DROP COLUMN is_new;
