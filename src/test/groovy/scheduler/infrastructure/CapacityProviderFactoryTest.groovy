package scheduler.infrastructure

import scheduler.capacity.InGroupProvider
import scheduler.capacity.NameContainsProvider
import scheduler.capacity.NameNotContainsProvider
import scheduler.capacity.NameNotContainsProviderTest
import scheduler.capacity.StaticCapacityProvider
import spock.lang.Specification
import spock.lang.Unroll

class CapacityProviderFactoryTest extends Specification {

    def cut = new CapacityProviderFactory()

    def "shall throw when unrecognized skill"() {
        when:
        cut.create("dummy", 1.0f)

        then:
        thrown(CapacityProviderFactory.UnrecognizedSkillException)
    }

    def "shall create StaticCapacityProvider"() {
        when:
        def result = cut.create(description, capacity)

        then:
        ((StaticCapacityProvider) result).capacity == capacity

        where:
        description | capacity
        "any"       | 1.0f
        "Any"       | 1.5f
        "ANY"       | 2.5f
        "   aNy "   | 3.5f
    }

    def "shall throw when constructed without param"() {
        when:
        cut.create(description, 1.5f)

        then:
        thrown(CapacityProviderFactory.NoParameterException)

        where:
        description << ["containing",
                        "containing ",
                        "  CoNtaining",
                        "CONTaining   ",
                        "not containing ",
                        " Not  Containing  "]
    }

    def "shall create NameContainsProvider"() {
        when:
        def result = cut.create(description, capacity)

        then:
        ((NameContainsProvider) result).capacity == capacity
        ((NameContainsProvider) result).string == string

        where:
        description                   | string        | capacity
        "containing xyz "             | "xyz"         | 1.0f
        "  ConTAining   ZyX  "        | "ZyX"         | 1.5f
        "  CONTAININg   ZyX bauS  "   | "ZyX bauS"    | 2.5f
        "  CoNtaining   'aBC deF  ' " | "'aBC deF  '" | 3.5f
    }

    def "shall create NameNotContainsProvider"() {
        when:
        def result = cut.create(description, capacity)

        then:
        ((NameNotContainsProvider) result).capacity == capacity
        ((NameNotContainsProvider) result).string == string

        where:
        description                        | string        | capacity
        "not containing xyz "              | "xyz"         | 1.0f
        "  Not ConTAining   ZyX  "         | "ZyX"         | 1.5f
        "  NOT   CONTAININg   ZyX bauS  "  | "ZyX bauS"    | 2.5f
        "  NoT  CoNtaining   'aBC deF  ' " | "'aBC deF  '" | 3.5f
    }

    def "shall create InGroupProvider with NameContainsProvider"() {
        when:
        def result = cut.create(description, capacity)

        then:
        ((NameContainsProvider) ((InGroupProvider) result).provider).capacity == capacity
        ((NameContainsProvider) ((InGroupProvider) result).provider).string == string

        where:
        description                              | string        | capacity
        "in group containing xyz "               | "xyz"         | 1.0f
        "  In  Group  ConTAining   ZyX  "        | "ZyX"         | 1.5f
        "  IN  GROUP   CONTAININg   ZyX bauS  "  | "ZyX bauS"    | 2.5f
        "  In  GrOuP  CoNtaining   'aBC deF  ' " | "'aBC deF  '" | 3.5f
    }

    def "shall create InGroupProvider with NameNotContainsProvider"() {
        when:
        def result = cut.create(description, capacity)

        then:
        ((NameNotContainsProvider) ((InGroupProvider) result).provider).capacity == capacity
        ((NameNotContainsProvider) ((InGroupProvider) result).provider).string == string

        where:
        description                                   | string        | capacity
        "in group not containing xyz "                | "xyz"         | 1.0f
        "  In  Group  Not  ConTAining   ZyX  "        | "ZyX"         | 1.5f
        "  IN  GROUP  NOT   CONTAININg   ZyX bauS  "  | "ZyX bauS"    | 2.5f
        "  In  GrOuP  nOT  CoNtaining   'aBC deF  ' " | "'aBC deF  '" | 3.5f
    }

}
