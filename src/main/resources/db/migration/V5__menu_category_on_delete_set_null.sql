ALTER TABLE menu_items
    DROP CONSTRAINT IF EXISTS fk_menu_items_category;

ALTER TABLE menu_items
    ADD CONSTRAINT fk_menu_items_category
        FOREIGN KEY (category_id) REFERENCES menu_categories(id)
        ON DELETE SET NULL;

ALTER TABLE menu_items
    ADD COLUMN flagged_by UUID;

ALTER TABLE menu_items
    ADD CONSTRAINT fk_menu_items_flagged_by
        FOREIGN KEY (flagged_by) REFERENCES admins(id);
