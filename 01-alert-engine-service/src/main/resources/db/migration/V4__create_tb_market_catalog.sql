create table tb_market_catalog
(
    event_id   UUID                           not null,
    catalog    jsonb                          not null,
    updated_at timestamp(6) WITHOUT TIME ZONE not null,
    catalog_version    BIGINT                 not null,
    constraint pk_tb_market_catalog primary key (event_id)
);