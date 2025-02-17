create table item
(
    id         serial,
    tarkov_id  varchar(255) not null,
    name       varchar(255),
    updated    timestamp,
    image_link varchar(255),
    buy_price  integer,
    constraint pk_item__id primary key (id),
    constraint uk_item__tarkov_id unique (tarkov_id)
);

create table account
(
    id    serial,
    email varchar(255) not null,
    constraint pk_account__id primary key (id),
    constraint uk_account__username unique (email)
);

create table role
(
    id   serial,
    name varchar(255) not null,
    constraint pk_role__id primary key (id),
    constraint uk_role__name unique (name)
);

create table account_role
(
    account_id integer not null,
    role_id    integer not null,
    constraint pk_account_role__account_id__role_id primary key (account_id, role_id),
    constraint fk_account_role__account_id foreign key (account_id) references account,
    constraint fk_account_role__role_id foreign key (role_id) references role
);

create table type
(
    id   serial,
    name varchar(255) not null,
    constraint pk_type__id primary key (id),
    constraint uk_type__name unique (name)
);

create table item_type
(
    item_id integer not null,
    type_id integer not null,
    constraint pk_item_type__item_id__type_id primary key (item_id, type_id),
    constraint fk_item_type__item_id foreign key (item_id) references item,
    constraint fk_item_type__type_id foreign key (type_id) references type
);

create table key
(
    id      serial,
    item_id integer not null,
    uses    integer not null,
    constraint pk_key__id primary key (id),
    constraint uk_key__item_id unique (item_id),
    constraint fk_key__item_id foreign key (item_id) references item
);

create table vendor
(
    id   serial,
    name varchar(255) not null,
    constraint pk_vendor__id primary key (id),
    constraint uk_vendor__name unique (name)
);

create table item_sale
(
    item_id    integer not null,
    sell_price integer not null,
    vendor_id  integer not null,
    constraint pk_item_sale__item_id__vendor_id primary key (item_id, vendor_id),
    constraint fk_item_sale__item_id foreign key (item_id) references item,
    constraint fk_item_sale__vendor_id foreign key (vendor_id) references vendor
);

create table key_report
(
    id          serial,
    reported_at timestamp not null,
    reported_by integer   not null,
    key_id      integer   not null,
    constraint pk_key_report__id primary key (id),
    constraint fk_key_report__reported_by foreign key (reported_by) references account,
    constraint fk_key_report__key_id foreign key (key_id) references key
);

create table loot_report
(
    id            serial,
    item_id       integer not null,
    count         integer not null,
    key_report_id integer not null,
    constraint pk_loot_report__id primary key (id),
    constraint fk_loot_report__item_id foreign key (item_id) references item,
    constraint fk_loot_report__key_report_id foreign key (key_report_id) references key_report
);

create view item_type_view(id, types) as
SELECT
    item.id,
    array_to_string(array_agg(DISTINCT type.name), '|'::text) AS types
FROM
    item
        JOIN item_type
             ON item.id = item_type.item_id
        JOIN type
             ON item_type.type_id = type.id
GROUP BY
    item.id;

create view highest_item_price(item_id, vendor_id, highest_sell_price) as
SELECT
    ranked.item_id,
    ranked.vendor_id,
    ranked.highest_sell_price
FROM
    (SELECT
         item_sale.item_id,
         item_sale.vendor_id,
         max(item_sale.sell_price)                                                                    AS highest_sell_price,
         row_number() OVER (PARTITION BY item_sale.item_id ORDER BY (max(item_sale.sell_price)) DESC) AS rank
     FROM
         item_sale
     GROUP BY
         item_sale.item_id,
         item_sale.vendor_id) ranked
WHERE
    ranked.rank = 1;

create view item_grid_view(id, name, image_link, types, buy_price, sell_price, sell_at, buy_price_rouble, sell_price_rouble) as
SELECT
    item.id,
    item.name,
    item.image_link,
    item_type_view.types,
    COALESCE(item.buy_price, 0)                                                                                    AS buy_price,
    COALESCE(highest_item_price.highest_sell_price, 0)                                                             AS sell_price,
    vendor.name                                                                                                    AS sell_at,
    '₽ '::text || COALESCE(item.buy_price::character varying, '0'::character varying)::text                        AS buy_price_rouble,
    '₽ '::text || COALESCE(highest_item_price.highest_sell_price::character varying, '0'::character varying)::text AS sell_price_rouble
FROM
    item
        JOIN item_type
             ON item.id = item_type.item_id
        JOIN type
             ON item_type.type_id = type.id
        JOIN item_type_view
             ON item_type_view.id = item.id
        JOIN highest_item_price
             ON highest_item_price.item_id = item.id
        JOIN vendor
             ON vendor.id = highest_item_price.vendor_id
GROUP BY
    item.id,
    highest_item_price.highest_sell_price,
    item_type_view.types,
    vendor.name;

create view key_grid_view(id, name, image_link, uses, buy_price, price_per_use, buy_price_rouble, price_per_use_rouble) as
SELECT
    key.id,
    item.name,
    item.image_link,
    key.uses,
    COALESCE(item.buy_price, 0)                                                                          AS buy_price,
    COALESCE(item.buy_price / key.uses, 0)                                                               AS price_per_use,
    '₽ '::text || COALESCE(item.buy_price::character varying, '0'::character varying)::text              AS buy_price_rouble,
    '₽ '::text || COALESCE((item.buy_price / key.uses)::character varying, '0'::character varying)::text AS price_per_use_rouble
FROM
    key
        JOIN item
             ON item.id = key.item_id;

create view loot_report_view(id, item_id, count, key_report_id, loot_value, image_link, name) as
SELECT
    loot_report.id,
    loot_report.item_id,
    loot_report.count,
    loot_report.key_report_id,
    loot_report.count * highest_item_price.highest_sell_price AS loot_value,
    item.image_link,
    item.name
FROM
    loot_report
        JOIN highest_item_price
             ON highest_item_price.item_id = loot_report.item_id
        JOIN item
             ON item.id = loot_report.item_id;

create view latest_report_view(id, reported_at, reported_by, key_id, name, item_count, total_value, image_link) as
SELECT
    key_report.id,
    key_report.reported_at,
    key_report.reported_by,
    key_report.key_id,
    item.name,
    sum(loot_report.count)           AS item_count,
    sum(loot_report_view.loot_value) AS total_value,
    item.image_link
FROM
    key_report
        JOIN account
             ON account.id = key_report.reported_by
        JOIN loot_report
             ON loot_report.key_report_id = key_report.id
        JOIN loot_report_view
             ON loot_report_view.key_report_id = key_report.id
        JOIN key
             ON key.id = key_report.key_id
        JOIN item
             ON item.id = key.item_id
GROUP BY
    key_report.id,
    item.name,
    item.image_link
ORDER BY
    key_report.id DESC
LIMIT 100;

create view key_report_view(id, reported_at, reported_by, key_id, name, item_count, total_value, image_link) as
SELECT
    key_report.id,
    key_report.reported_at,
    key_report.reported_by,
    key_report.key_id,
    item.name,
    sum(loot_report.count)           AS item_count,
    sum(loot_report_view.loot_value) AS total_value,
    item.image_link
FROM
    key_report
        JOIN account
             ON account.id = key_report.reported_by
        JOIN loot_report
             ON loot_report.key_report_id = key_report.id
        JOIN loot_report_view
             ON loot_report_view.key_report_id = key_report.id
        JOIN key
             ON key.id = key_report.key_id
        JOIN item
             ON item.id = key.item_id
GROUP BY
    key_report.id,
    item.name,
    item.image_link;

