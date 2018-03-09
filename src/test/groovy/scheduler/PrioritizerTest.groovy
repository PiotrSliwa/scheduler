package scheduler

import spock.lang.Specification

class PrioritizerTest extends Specification {

    def cut = new Prioritizer()
    def root = new Item('root')
    def item1 = new Item('id1')
    def item2 = new Item('id2')
    def item3 = new Item('id3')

    def "shall return on-item result return when one-node tree is given"() {
        when:
        def result = cut.prioritize([item1])

        then:
        result[0] == [item1]
    }

    def "shall return leaves in parallel when no other relations given"() {
        given:
        root.addChild(item1)
        root.addChild(item2)
        root.addChild(item3)

        when:
        def result = cut.prioritize([root, item1, item2, item3])

        then:
        result.size() == 1
        result[0].containsAll([item1, item2, item3])
    }

    def "shall prioritize linear dependencies"() {
        given:
        root.addChild(item1)
        root.addChild(item2)
        root.addChild(item3)
        item1.addDependency(item2)
        item2.addDependency(item3)

        when:
        def result = cut.prioritize([item2, root, item3, item1])

        then:
        result.size() == 3
        result[0] == [item3]
        result[1] == [item2]
        result[2] == [root, item1]
    }

    def "shall prioritize groups"() {
        given:
        root.addChild(item1)
        root.addChild(item2)
        def item11 = new Item('id11')
        def item12 = new Item('id12')
        def item21 = new Item('id21')
        def item22 = new Item('id22')
        item1.addChild(item11)
        item1.addChild(item12)
        item2.addChild(item21)
        item2.addChild(item22)
        item1.addDependency(item2)

        when:
        def result = cut.prioritize([root, item1, item2, item11, item12, item21, item22])

        then:
        result.size() == 2
        result[0].containsAll([item21, item22])
        result[1].containsAll([item11, item12])
    }

    def "shall not merge groups"() {
        given:
        root.addChild(item1)
        root.addChild(item2)
        def item11 = new Item('id11')
        def item12 = new Item('id12')
        def item21 = new Item('id21')
        def item22 = new Item('id22')
        item1.addChild(item11)
        item1.addChild(item12)
        item2.addChild(item21)
        item2.addChild(item22)
        item1.addDependency(item2)
        item11.addDependency(item12)

        when:
        def result = cut.prioritize([root, item1, item2, item11, item12, item21, item22])

        then:
        result.size() == 3
        result[0].containsAll([item2, item21, item22])
        result[1] == [item12]
        result[2].containsAll([root, item1, item11])
    }

    def "shall prioritize items according to relations"() {
        given:
        def root = new Item('root')
        def item1 = new Item('item1')
        def item11 = new Item('item11')
        def item12 = new Item('item12')
        def item121 = new Item('item121')
        def item122 = new Item('item122')
        def item13 = new Item('item13')
        def item2 = new Item('item2')
        def item21 = new Item('item21')
        def item22 = new Item('item22')
        def item3 = new Item('item3')
        root.addChild(item1)
        root.addChild(item2)
        root.addChild(item3)
        item1.addChild(item11)
        item1.addChild(item12)
        item1.addChild(item13)
        item12.addChild(item121)
        item12.addChild(item122)
        item2.addChild(item21)
        item2.addChild(item22)
        item11.addDependency(item13)
        item3.addDependency(item11)
        item2.addDependency(item3)
        item12.addDependency(item2)
        item122.addDependency(item121)

        when:
        def result = cut.prioritize([root, item1, item11, item12, item121, item122, item13, item2, item21, item22, item3])

        then:
        result.size() == 6
        result[5].containsAll([root, item1, item12, item122])
        result[4] == [item121]
        result[3].containsAll([item2, item21, item22])
        result[2] == [item3]
        result[1] == [item11]
        result[0] == [item13]
    }

}
