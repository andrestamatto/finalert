package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository;

import br.dev.andrestamatto.finalert.alertengineservice.application.ports.out.repository.MarketCatalogRepository;
import br.dev.andrestamatto.finalert.alertengineservice.domain.MarketCatalog;
import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.repository.mapper.MarketCatalogEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaMarketCatalogtRepositoryAdapter implements MarketCatalogRepository {

    private final SpringDataMarketCatalogRepository repository;
    private final MarketCatalogEntityMapper marketCatalogEntityMapper;

    public JpaMarketCatalogtRepositoryAdapter(SpringDataMarketCatalogRepository repository, MarketCatalogEntityMapper marketCatalogEntityMapper) {
        this.repository = repository;
        this.marketCatalogEntityMapper = marketCatalogEntityMapper;
    }

    @Override
    public void save(MarketCatalog catalog) {
        repository.save(
                marketCatalogEntityMapper.toEntity(catalog)
        );
    }

    @Override
    public Optional<MarketCatalog> findCurrent() {
        return repository.findFirstByOrderByCatalogVersionDesc()
                    .map(marketCatalogEntityMapper::toDomain);
    }

    @Override
    public boolean containsPair(String symbol) {
        return this.findCurrent()
                .map((catalog) -> catalog.pairs().containsKey(symbol))
                .orElse(false);
    }
}
