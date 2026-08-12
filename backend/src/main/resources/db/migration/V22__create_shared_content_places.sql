create table shared_content_places (
    id bigserial primary key,
    shared_content_id bigint not null references shared_contents(id) on delete cascade,
    display_order integer not null,
    name varchar(200) not null,
    category varchar(100),
    address varchar(500),
    latitude double precision,
    longitude double precision,
    image_url varchar(2048),
    saved_place_id bigint references user_saved_places(id) on delete set null,
    created_at timestamptz not null default now(),
    constraint uk_shared_content_place_order unique (shared_content_id, display_order)
);

create index idx_shared_content_places_content
    on shared_content_places(shared_content_id, display_order);
