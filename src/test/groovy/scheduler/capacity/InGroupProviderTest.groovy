package scheduler.capacity

import scheduler.Item
import spock.lang.Specification


class InGroupProviderTest extends Specification {

    def "shall traverse up and return 0 if no response"() {
        given:
        def provider = Mock(CapacityProvider) {
            provide(_) >> 0.0f
        }
        def cut = new InGroupProvider(provider)

        and:
        def grandparent = Mock(Item)
        def parent = Mock(Item)
        def item = Mock(Item)

        when:
        def result = cut.provide(item)

        then:
        1 * item.getParent() >> parent
        1 * parent.getParent() >> grandparent
        1 * grandparent.getParent() >> null
        result == 0.0f
    }

    def "shall return max ancestor's response"() {
        given:
        def provider = Mock(CapacityProvider)
        def cut = new InGroupProvider(provider)

        and:
        def grandgrandparent = Mock(Item) { getParent() >> null }
        def grandparent = Mock(Item) { getParent() >> grandgrandparent }
        def parent = Mock(Item) { getParent() >> grandparent }
        def item = Mock(Item) { getParent() >> parent }

        when:
        def result = cut.provide(item)

        then:
        0 * provider.provide(item)
        1 * provider.provide(parent) >> 0.5f
        1 * provider.provide(grandparent) >> 1.5f
        1 * provider.provide(grandgrandparent) >> 1.0f
        result == 1.5f
    }
}
