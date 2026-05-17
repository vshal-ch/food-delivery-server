DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'menu_item_flag_status') THEN
        CREATE TYPE menu_item_flag_status AS ENUM ('no_flag', 'flagged', 'submitted_for_review');
    END IF;
END $$;

ALTER TABLE menu_items
    RENAME COLUMN is_flagged TO flagged_status;

ALTER TABLE menu_items
    ALTER COLUMN flagged_status DROP DEFAULT,
    ALTER COLUMN flagged_status TYPE menu_item_flag_status
        USING CASE
            WHEN flagged_status IS TRUE THEN 'flagged'::menu_item_flag_status
            ELSE 'no_flag'::menu_item_flag_status
        END,
    ALTER COLUMN flagged_status SET DEFAULT 'no_flag',
    ALTER COLUMN flagged_status SET NOT NULL;
