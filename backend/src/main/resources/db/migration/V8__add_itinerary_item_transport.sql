ALTER TABLE itinerary_items
    ADD COLUMN transport_type_from_previous VARCHAR(30);

ALTER TABLE itinerary_items
    ADD CONSTRAINT chk_itinerary_item_transport
        CHECK (transport_type_from_previous IS NULL
            OR transport_type_from_previous IN ('WALKING', 'PUBLIC_TRANSIT', 'CAR'));
