package scheduler

import spock.lang.Specification

import static spock.util.matcher.HamcrestMatchers.closeTo

class ItemTest extends Specification {

    def item1 = new Item("id1")
    def item2 = new Item("id2")
    def item3 = new Item("id3")
    def item4 = new Item("id4")

    def "shall be equal when id is equal"() {
        when:
        def item1 = new Item(id1)
        def item2 = new Item(id2)

        then:
        item1 == item2

        where:
        id1 | id2 | name1 | name2 | size1 | size2
        "a" | "a" | "b"   | "b"   | 1     | 1
        "a" | "a" | "b"   | "c"   | 1     | 2
    }

    def "shall not be equal when id is not equal"() {
        when:
        def item1 = new Item(id1, new Item.Parameters(name1, size1, threads1))
        def item2 = new Item(id2)

        then:
        item1 != item2

        where:
        id1 | id2 | name1 | name2 | size1 | size2 | threads1 | threads2
        "a" | "b" | "c"   | "c"   | 1     | 1     | 1        | 1
        "a" | "b" | "c"   | "d"   | 1     | 2     | 2        | 2
    }

    def "shall add dependencies"() {
        when:
        item1.addDependency(item2)
        item1.addDependency(item3)

        then:
        item1.getDependencies() == [item2, item3]
    }

    def "shall add impacted items"() {
        when:
        item1.addDependency(item3)
        item2.addDependency(item3)

        then:
        item3.getImpacted() == [item1, item2]
    }

    def "shall throw in case of direct cyclic dependency and do not add the dependency"() {
        given:
        item1.addDependency(item2)
        item1.addDependency(item3)
        item1.addDependency(item4)

        when:
        item3.addDependency(item1)

        then:
        thrown(Item.CyclicDependencyException)
        item3.getDependencies().isEmpty()
    }

    def "shall throw in case of recursive cyclic dependency and do not add the dependency"() {
        given:
        item1.addDependency(item2)
        item2.addDependency(item3)
        item3.addDependency(item4)

        when:
        item4.addDependency(item1)

        then:
        thrown(Item.CyclicDependencyException)
        item4.getDependencies().isEmpty()
    }

    def "shall add children to list"() {
        when:
        item1.addChild(item2)
        item1.addChild(item3)

        then:
        item1.getChildren() == [item2, item3]
        item2.getParent() == item1
        item3.getParent() == item1
    }

    def "item's parent cannot be the item's child"() {
        given:
        item1.addChild(item2)
        item1.addChild(item3)
        item1.addChild(item4)

        when:
        item4.addChild(item1)

        then:
        thrown(Item.GenealogyException)
        item4.getChildren().isEmpty()
    }

    def "item's successor cannot be the item's predecessor"() {
        given:
        item1.addChild(item2)
        item2.addChild(item3)
        item3.addChild(item4)

        when:
        item4.addChild(item1)

        then:
        thrown(Item.GenealogyException)
        item4.getChildren().isEmpty()
    }

    def "item cannot be dependent from its predecessor during addDependency"() {
        given:
        item1.addChild(item2)

        when:
        item2.addDependency(item1)

        then:
        thrown(Item.DependencyCannotBePredecessorException)
        item2.getDependencies().isEmpty()
    }

    def "item cannot be dependent from its successor during addDependency"() {
        given:
        item1.addChild(item2)

        when:
        item1.addDependency(item2)

        then:
        thrown(Item.DependentFromSuccessorException)
        item1.getDependencies().isEmpty()
    }

    def "item cannot be dependent from its predecessor during addChild"() {
        given:
        item1.addDependency(item2)

        when:
        item2.addChild(item1)

        then:
        thrown(Item.DependentFromSuccessorException)
        item2.getChildren().isEmpty()
    }

    def "item cannot be dependent from its successor during addChild"() {
        given:
        item1.addDependency(item2)

        when:
        item1.addChild(item2)

        then:
        thrown(Item.DependentFromPredecessorException)
        item1.getChildren().isEmpty()
    }

    def "isDependentFrom shall return false for non-dependent-from item"() {
        when:
        def result =  item1.isDependentFrom(item2)

        then:
        !result
    }

    def "isDependentFrom shall return true for direct dependency"() {
        given:
        item1.addDependency(item2)
        item1.addDependency(item3)

        when:
        def result =  item1.isDependentFrom(item2)

        then:
        result
    }

    def "isDependentFrom shall return true for indirect dependency"() {
        given:
        item1.addDependency(item2)
        item2.addDependency(item3)

        when:
        def result =  item1.isDependentFrom(item3)

        then:
        result
    }

    def "isDependentFrom shall return true for item's offspring"() {
        given:
        item1.addDependency(item2)
        item2.addChild(item3)
        item3.addChild(item4)

        when:
        def result =  item1.isDependentFrom(item4)

        then:
        result
    }

    def "getTotalDependentSize shall return 0 when no dependencies"() {
        when:
        def result = item1.getTotalDependentSize()

        then:
        result closeTo(0.0f, 0.001f)
    }

    def "getTotalDependentSize shall return summed impacted chain's size"() {
        given:
        item1.parameters.size = 9.2f
        item1.addDependency(item2)
        item2.parameters.size = 1.5f
        item2.addDependency(item3)
        item3.parameters.size = 6.23f

        when:
        def result = item3.getTotalDependentSize()

        then:
        result closeTo(item1.parameters.size + item2.parameters.size, 0.001f)
    }

    def "getTotalDependentSize shall return impacted item's summed children sizes"() {
        given:
        item1.addDependency(item2)
        item1.addChild(item3)
        item1.addChild(item4)
        item2.parameters.size = 9.2f
        item3.parameters.size = 1.5f
        item4.parameters.size = 4.89f

        when:
        def result = item2.getTotalDependentSize()

        then:
        result closeTo(item3.parameters.size + item4.parameters.size, 0.001f)
    }

    def "shall throw when item with non-null size is added when a predecessor has non-null size"() {
        given:
        def item1 = new Item("id1", new Item.Parameters("name", 1.0f, 1))
        def item2 = new Item("id2")
        def item3 = new Item("id3", new Item.Parameters("name", 1.0f, 1))
        item1.addChild(item2)

        when:
        item2.addChild(item3)

        then:
        thrown(Item.PredecessorAlreadySizedException)
    }

}
