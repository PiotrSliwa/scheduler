package scheduler.capacity

import scheduler.Item
import spock.lang.Specification


class NameContainsProviderTest extends Specification {

    def "provide"() {
        when:
        def item = Mock(Item) {
            getParameters() >> new Item.Parameters(itemName, 1.0f, 1)
        }
        def cut = new NameContainsProvider(matchingName, capacity)

        then:
        cut.provide(item) == expected

        where:
        itemName          | matchingName | capacity | expected
        'dummy'           | 'other'      | 0.0f     | 0.0f
        'dummy'           | 'other'      | 1.0f     | 0.0f
        'dummy'           | 'dummy'      | 0.0f     | 0.0f
        'dummy'           | 'dummy'      | 1.0f     | 1.0f
        'long dummy name' | 'dummy'      | 2.0f     | 2.0f
    }
}
