ALTER TABLE place_changes
    DROP CONSTRAINT IF EXISTS fk_place_changes_reviewed_by;

ALTER TABLE place_changes
    ADD CONSTRAINT fk_place_changes_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES admins(id);

ALTER TABLE places
    ADD COLUMN reviewed_by UUID,
    ADD COLUMN reviewed_at TIMESTAMP,
    ADD COLUMN review_comments TEXT;

ALTER TABLE places
    ADD CONSTRAINT fk_places_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES admins(id);
