package br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.out.outbox;

import br.dev.andrestamatto.finalert.alertengineservice.infrastructure.adapter.outbox.RabbitMqMessageBrokerPublisher;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Resilience4jCircuitBreakerIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AopAutoConfiguration.class,
                    CircuitBreakerAutoConfiguration.class
            ))
            .withPropertyValues(
                    "resilience4j.circuitbreaker.instances.rabbitPublisher.sliding-window-type=COUNT_BASED",
                    "resilience4j.circuitbreaker.instances.rabbitPublisher.sliding-window-size=2",
                    "resilience4j.circuitbreaker.instances.rabbitPublisher.minimum-number-of-calls=2",
                    "resilience4j.circuitbreaker.instances.rabbitPublisher.failure-rate-threshold=100",
                    "resilience4j.circuitbreaker.instances.rabbitPublisher.wait-duration-in-open-state=30s"
            )
            .withBean(AlwaysFailingPublisher.class);

    @Test
    void appliesTheConfiguredCircuitBreakerThroughSpringAop() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var publisher = context.getBean(AlwaysFailingPublisher.class);
            assertThat(AopUtils.isAopProxy(publisher)).isTrue();

            assertThatThrownBy(publisher::publish).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(publisher::publish).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(publisher::publish).isInstanceOf(CallNotPermittedException.class);
        });
    }

    static class AlwaysFailingPublisher {

        @CircuitBreaker(name = RabbitMqMessageBrokerPublisher.CIRCUIT_BREAKER_NAME)
        public void publish() {
            throw new IllegalStateException("RabbitMQ unavailable");
        }
    }
}
