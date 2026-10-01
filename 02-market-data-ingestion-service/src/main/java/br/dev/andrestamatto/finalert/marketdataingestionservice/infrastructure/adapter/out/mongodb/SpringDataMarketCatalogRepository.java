package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.mongodb;

import javax.xml.catalog.Catalog;

public interface SpringDataMarketCatalogRepository {
    void replaceCatalog(Catalog catalog);
}
