package br.dev.andrestamatto.finalert.marketdataingestionservice.infrastructure.adapter.out.awesomeapi.feignclients.response;

import br.dev.andrestamatto.finalert.marketdataingestionservice.domain.MarketPrice;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AwesomeApiQuoteResponse(
        String code,
        String codein,
        String name,
        BigDecimal high,
        BigDecimal low,
        @JsonProperty("varBid") BigDecimal variation,
        @JsonProperty("pctChange") BigDecimal percentChange,
        BigDecimal bid,
        BigDecimal ask,
        String timestamp,
        @JsonProperty("create_date") String createdAt
) {
    public MarketPrice toDomain() {
        return new MarketPrice(
                code,
                codein,
                name,
                high,
                low,
                variation,
                percentChange,
                bid,
                ask,
                Instant.ofEpochSecond(Long.parseLong(timestamp)),
                LocalDateTime.parse(
                        createdAt,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
        );
    }

}
