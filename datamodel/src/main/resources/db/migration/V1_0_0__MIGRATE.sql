CREATE TYPE GAME_MODE AS ENUM ('PvP', 'PvE');

-- TABLE: ACCOUNT
CREATE TABLE account
(
    id        SERIAL       NOT NULL,
    email     VARCHAR(255) NOT NULL,
    game_mode GAME_MODE    NOT NULL DEFAULT 'PvP',
    CONSTRAINT pk_account__id PRIMARY KEY (id),
    CONSTRAINT uk_account__username UNIQUE (email)
);

-- TABLE: ROLE
CREATE TABLE role
(
    id   SERIAL       NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_role__id PRIMARY KEY (id),
    CONSTRAINT uk_role__name UNIQUE (name)
);

-- TABLE: ACCOUNT_ROLE
CREATE TABLE account_role
(
    account_id INTEGER NOT NULL,
    role_id    INTEGER NOT NULL,
    CONSTRAINT pk_account_role__account_id__role_id PRIMARY KEY (account_id, role_id),
    CONSTRAINT fk_account_role__account_id FOREIGN KEY (account_id) REFERENCES account,
    CONSTRAINT fk_account_role__role_id FOREIGN KEY (role_id) REFERENCES role
);

-- TABLE: ITEM
CREATE TABLE item
(
    id                        SERIAL       NOT NULL,
    tarkov_market_id          VARCHAR(255) NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    banned_on_flea            BOOL         NOT NULL,
    have_market_data          BOOL         NOT NULL,
    short_name                VARCHAR(255) NOT NULL,
    pve_price                 INTEGER      NOT NULL,
    pvp_price                 INTEGER      NOT NULL,
    pve_base_price            INTEGER      NOT NULL,
    pvp_base_price            INTEGER      NOT NULL,
    pve_avg24h_price          INTEGER      NOT NULL,
    pvp_avg24h_price          INTEGER      NOT NULL,
    pve_avg7days_price        INTEGER      NOT NULL,
    pvp_avg7days_price        INTEGER      NOT NULL,
    pve_trader_name           VARCHAR(255) NOT NULL,
    pvp_trader_name           VARCHAR(255) NOT NULL,
    pve_trader_price          INTEGER      NOT NULL,
    pvp_trader_price          INTEGER      NOT NULL,
    pve_trader_price_currency VARCHAR(255) NOT NULL,
    pvp_trader_price_currency VARCHAR(255) NOT NULL,
    pve_trader_price_rouble   INTEGER      NOT NULL,
    pvp_trader_price_rouble   INTEGER      NOT NULL,
    pve_diff24h               FLOAT        NOT NULL,
    pvp_diff24h               FLOAT        NOT NULL,
    pve_diff7days             FLOAT        NOT NULL,
    pvp_diff7days             FLOAT        NOT NULL,
    updated                   TIMESTAMP    NOT NULL,
    slots                     INTEGER      NOT NULL,
    icon                      VARCHAR(255) NOT NULL,
    link                      VARCHAR(255) NOT NULL,
    wiki_link                 VARCHAR(255) NOT NULL,
    image_link                VARCHAR(255) NOT NULL,
    image_big_link            VARCHAR(255) NOT NULL,
    tarkov_id                 VARCHAR(255) NOT NULL,
    is_functional             BOOL         NOT NULL,
    reference                 VARCHAR(255) NOT NULL,
    CONSTRAINT pk_item__id PRIMARY KEY (id),
    CONSTRAINT uk_item__bsg_tarkov_id UNIQUE (tarkov_id),
    CONSTRAINT uk_item__tarkov_market_id UNIQUE (tarkov_market_id)
);

-- TABLE: TAG
CREATE TABLE tag
(
    id   SERIAL       NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_tag__id PRIMARY KEY (id),
    CONSTRAINT uk_tag__name UNIQUE (name)
);

-- TABLE: ITEM_TAG
CREATE TABLE item_tag
(
    item_id INTEGER NOT NULL,
    tag_id  INTEGER NOT NULL,
    CONSTRAINT pk_item_tag__item_id__tag_id PRIMARY KEY (item_id, tag_id),
    CONSTRAINT fk_item_tag__item_id FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_item_tag__tag_id FOREIGN KEY (tag_id) REFERENCES tag (id)
);

-- TABLE: KEY
CREATE TABLE key
(
    id      SERIAL  NOT NULL,
    item_id INTEGER NOT NULL,
    uses    INTEGER NOT NULL,
    CONSTRAINT pk_key__id PRIMARY KEY (id),
    CONSTRAINT uk_key__item_id UNIQUE (item_id),
    CONSTRAINT fk_key__item_id FOREIGN KEY (item_id) REFERENCES item (id)
);

-- TABLE: KEY_REPORT
CREATE TABLE key_report
(
    id          SERIAL    NOT NULL,
    key_id      INTEGER   NOT NULL,
    reported_at TIMESTAMP NOT NULL,
    account_id  INTEGER   NOT NULL,
    mode        GAME_MODE NOT NULL,
    CONSTRAINT pk_key_report__id PRIMARY KEY (id),
    CONSTRAINT fk_key_report__key_id FOREIGN KEY (key_id) REFERENCES key (id),
    CONSTRAINT fk_key_report__account_id FOREIGN KEY (account_id) REFERENCES account (id)
);

-- TABLE: LOOT_REPORT
CREATE TABLE loot_report
(
    id            SERIAL  NOT NULL,
    item_id       INTEGER NOT NULL,
    count         INTEGER NOT NULL,
    key_report_id INTEGER NOT NULL,
    CONSTRAINT pk_loot_report__id PRIMARY KEY (id),
    CONSTRAINT pk_loot_report__item_id FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT pk_loot_report__key_report_id FOREIGN KEY (key_report_id) REFERENCES key_report (id)
);

-- VIEW: TAG_VIEW
CREATE VIEW tag_view AS
SELECT tag.id                      AS tag_id,
       tag.name,
       REPLACE(tag.name, '_', ' ') AS clean_name
FROM tag;

-- VIEW: ITEM_TAG_VIEW
CREATE VIEW item_tag_view AS
SELECT item_tag.item_id,
       ARRAY_TO_STRING(ARRAY_AGG(DISTINCT tag_view.clean_name), '|'::TEXT) AS tags
FROM item_tag
         JOIN tag_view
              ON item_tag.tag_id = tag_view.tag_id
GROUP BY item_tag.item_id;

-- VIEW: ITEM_PRICE_VIEW
CREATE VIEW item_price_view AS
SELECT item.id                                                                    AS item_id,
       item.banned_on_flea                                                        AS item_banned_on_flea,
       item.pve_trader_price                                                      AS pve_trader_value,
       item.pve_trader_price_currency                                             AS pve_trader_value_currency,
       CASE WHEN item.banned_on_flea = TRUE THEN 0 ELSE item.pve_avg24h_price END AS pve_flea_value,
       item.pvp_trader_price                                                      AS pvp_trader_value,
       item.pvp_trader_price_currency                                             AS pvp_trader_value_currency,
       CASE WHEN item.banned_on_flea = TRUE THEN 0 ELSE item.pvp_avg24h_price END AS pvp_flea_value
FROM item;

-- VIEW: ITEM_GRID_VIEW
CREATE VIEW item_grid_view AS
SELECT item.id                                                    AS item_id,
       COALESCE(NULLIF(TRIM(item.icon), ''), item.image_big_link) AS image_link,
       item.name,
       item_price_view.item_banned_on_flea,
       item_price_view.pve_trader_value,
       item_price_view.pve_trader_value_currency,
       item_price_view.pve_flea_value,
       item_price_view.pvp_trader_value,
       item_price_view.pvp_trader_value_currency,
       item_price_view.pvp_flea_value,
       item_tag_view.tags,
       item.wiki_link,
       item.link                                                  AS market_link
FROM item
         JOIN item_tag_view
              ON item.id = item_tag_view.item_id
         JOIN item_price_view
              ON item.id = public.item_price_view.item_id;

-- VIEW: KEY_LOCATION_VIEW
CREATE VIEW key_location_view AS
SELECT key.id                                                              AS key_id,
       ARRAY_TO_STRING(ARRAY_AGG(DISTINCT tag_view.clean_name), '|'::TEXT) AS locations
FROM key
         JOIN item_tag
              ON key.item_id = item_tag.item_id
         JOIN tag_view
              ON item_tag.tag_id = tag_view.tag_id
WHERE tag_view.name IN ('Customs', 'Factory', 'Interchange', 'Reserve', 'Shoreline', 'Streets_of_Tarkov', 'The_Lab', 'Woods')
GROUP BY key.id;

-- VIEW: KEY_GRID_VIEW
CREATE VIEW key_grid_view AS
SELECT item.id                                                    AS item_id,
       COALESCE(NULLIF(TRIM(item.icon), ''), item.image_big_link) AS image_link,
       item.name,
       item.banned_on_flea                                        AS item_banned_on_flea,
       item_price_view.pve_flea_value                             AS pve_flea_price,
       item_price_view.pvp_flea_value                             AS pvp_flea_price,
       item_price_view.pve_flea_value / uses                      AS pve_flea_price_per_use,
       item_price_view.pvp_flea_value / uses                      AS pvp_flea_price_per_use,
       item.wiki_link,
       item.link                                                  AS market_link,
       key.uses,
       key_location_view.locations
FROM key
         JOIN item
              ON item.id = key.item_id
         JOIN item_price_view
              ON item.id = public.item_price_view.item_id
         JOIN key_location_view
              ON key.id = key_location_view.key_id;

-- VIEW: KEY_REPORT_VIEW
CREATE VIEW key_report_view AS
SELECT key_report.id                                                                    AS key_report_id,
       item.name                                                                        AS key_name,
       COALESCE(NULLIF(TRIM(BOTH FROM item.icon), ''::TEXT), item.image_big_link::TEXT) AS image_link,
       key_report.reported_at,
       key_report.account_id                                                            AS reported_by,
       SUM(loot_report.count)                                                           AS item_count,
       key_report.mode                                                                  AS game_mode
FROM key_report
         JOIN loot_report
              ON key_report.id = loot_report.key_report_id
         JOIN key
              ON key_report.key_id = public.key.id
         JOIN item
              ON key.item_id = item.id
GROUP BY key_report.id,
         item.name,
         item.icon,
         item.image_big_link;

-- VIEW: LOOT_REPORT_VIEW
CREATE VIEW loot_report_view AS
SELECT loot_report.id                                                                   AS loot_report_id,
       loot_report.key_report_id,
       loot_report.count,
       item.id                                                                          AS item_id,
       item.name,
       COALESCE(NULLIF(TRIM(BOTH FROM item.icon), ''::TEXT), item.image_big_link::TEXT) AS image_link
FROM loot_report
         JOIN item
              ON loot_report.item_id = item.id;