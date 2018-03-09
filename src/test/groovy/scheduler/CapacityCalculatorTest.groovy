package scheduler

import scheduler.capacity.CapacityCalculator
import scheduler.capacity.CapacityProvider
import spock.lang.Specification

import java.security.Provider


class CapacityCalculatorTest extends Specification {

    def item = Mock(Item)

    def "shall return 0 by default"() {
        when:
        def cut = new CapacityCalculator([])

        then:
        cut.calculate(item) == 0.0f
    }

    def "shall return 0 if provider returns 0"() {
        when:
        def provider = Mock(CapacityProvider) {
            provide(_) >> 0.0f
        }
        def cut = new CapacityCalculator([provider])

        then:
        cut.calculate(item) == 0.0f
    }

    def "shall return max responding capacity"() {
        when:
        def provider1 = Mock(CapacityProvider) {
            provide(item) >> 0.5f
        }
        def provider2 = Mock(CapacityProvider) {
            provide(item) >> 1.5f
        }
        def provider3 = Mock(CapacityProvider) {
            provide(item) >> 1.0f
        }
        def cut = new CapacityCalculator([provider1, provider2, provider3])

        then:
        cut.calculate(item) == 1.5f
    }

}
