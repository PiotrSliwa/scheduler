package scheduler

import javafx.util.Pair
import scheduler.capacity.CapacityCalculator
import spock.lang.Specification

class TimelineFactoryTest extends Specification {

    def fullCapacityCalculator = Mock(CapacityCalculator) {
        calculate(_) >> 1.0f
    }
    def resource1 = new ProjectResource('resource1', fullCapacityCalculator, [])
    def resource2 = new ProjectResource('resource2', fullCapacityCalculator, [])
    def resource3 = new ProjectResource('resource3', fullCapacityCalculator, [])

    void assertResources(Iterable<WorkPackage> result, ProjectResource... resources) {
        def toCheck = resources.toList();
        for (def pkg : result)
            assert toCheck.remove(pkg.resource), "Cannot find resource ${pkg.item}"
        assert toCheck.isEmpty(), "Cannot match resources ${toCheck}"
    }

    void assertItems(Iterable<WorkPackage> result, Item... items) {
        def toCheck = items.toList();
        for (def pkg : result)
            assert toCheck.remove(pkg.item), "Cannot find item ${pkg.item}"
        assert toCheck.isEmpty(), "Cannot match items ${toCheck}"
    }

    def "one item"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def cut = new TimelineFactory(1, [resource1])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 1
        result[0] == [new WorkPackage(resource1, item, 1.0f)]
    }

    def "one item in two threads"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 2))
        def cut = new TimelineFactory(0.5f, [resource1, resource2])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 1
        result[0] == [new WorkPackage(resource1, item, 0.5f),
                      new WorkPackage(resource2, item, 0.5f)]
    }

    def "resource with half the capacity shall perform a task twice as long"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def capacityCalculator = Mock(CapacityCalculator) {
            calculate(_) >> 0.5f
        }
        def resource = new ProjectResource('resource', capacityCalculator, [])
        def cut = new TimelineFactory(1, [resource])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 2
        result[0] == [new WorkPackage(resource, item, 0.5f)]
        result[1] == [new WorkPackage(resource, item, 0.5f)]
    }

    def "two items"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 1, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def cut = new TimelineFactory(1, [resource1])

        when:
        def result = cut.create([[item1, item2]])

        then:
        result.size() == 2
        result[0] == [new WorkPackage(resource1, item1, 1.0f)]
        result[1] == [new WorkPackage(resource1, item2, 1.0f)]
    }

    def "shall not include item without size or threads"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', size, threads))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def cut = new TimelineFactory(1, [resource1])

        when:
        def result = cut.create([[item1, item2]])

        then:
        result.size() == 1
        result[0] == [new WorkPackage(resource1, item2, 1.0f)]

        where:
        size | threads
        null | null
        1    | null
        null | 1
        0    | 0
        1    | 0
        0    | 1
    }

    def "one long item"() {
        given:
        def item = new Item('item1', new Item.Parameters('name', 2, 1))
        def cut = new TimelineFactory(1, [resource1])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 2
        result[0] == [new WorkPackage(resource1, item, 1.0f)]
        result[1] == [new WorkPackage(resource1, item, 1.0f)]
    }

    def "one long item in two threads"() {
        given:
        def item = new Item('item1', new Item.Parameters('name', 2, 2))
        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 1
        result[0] == [new WorkPackage(resource1, item, 1.0f),
                      new WorkPackage(resource2, item, 1.0f)]
    }

    def "too many resources"() {
        given:
        def item = new Item('item1', new Item.Parameters('name', 2, 1))
        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 2
        !result[0].any { t -> t.resource.equals(resource2) }
        !result[1].any { t -> t.resource.equals(resource2) }
    }

    def "more threads than resources"() {
        given:
        def item = new Item('item1', new Item.Parameters('name', 3, 3))
        def cut = new TimelineFactory(1, [resource1])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 3
        result[0] == [new WorkPackage(resource1, item, 1.0f)]
        result[1] == [new WorkPackage(resource1, item, 1.0f)]
        result[2] == [new WorkPackage(resource1, item, 1.0f)]
    }

    def "two groups of dependent tasks"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 2, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 2, 2))
        item2.addDependency(item1)
        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item1], [item2]])

        then:
        result.size() == 3
        result[0] == [new WorkPackage(resource1, item1, 1.0f)]
        result[1] == [new WorkPackage(resource1, item1, 1.0f)]
        result[2] == [new WorkPackage(resource1, item2, 1.0f),
                      new WorkPackage(resource2, item2, 1.0f)]
    }

    def "high resolution"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def cut = new TimelineFactory(0.25f, [resource1])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 4
        result[0] == [new WorkPackage(resource1, item, 0.25f)]
        result[1] == [new WorkPackage(resource1, item, 0.25f)]
        result[2] == [new WorkPackage(resource1, item, 0.25f)]
        result[3] == [new WorkPackage(resource1, item, 0.25f)]
    }

    def "two independent streams for two resources"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 2, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def item3 = new Item('item3', new Item.Parameters('name', 1, 1))

        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item1, item2], [item3]])

        then:
        result[0] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item2, 1.0f)]
        result[1] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item3, 1.0f)]
    }

    def "two independent streams for two resources with strange alignment"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 2, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def item3 = new Item('item3', new Item.Parameters('name', 1, 1))

        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item1], [item2], [item3]]) //FIXME: replace 2-d collection with a simple one

        then:
        result[0] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item2, 1.0f)]
        result[1] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item3, 1.0f)]
    }

    def "two independent streams with too many resources"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 3, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def item3 = new Item('item3', new Item.Parameters('name', 1, 1))
        def item4 = new Item('item4', new Item.Parameters('name', 1, 1))
        item4.addDependency(item3)
        item3.addDependency(item2)

        def cut = new TimelineFactory(1, [resource1, resource2, resource3])

        when:
        def result = cut.create([[item1, item2], [item3], [item4]])

        then:
        assertResources(result[0], resource1, resource2)
        assertItems(result[0], item1, item2)
        assertResources(result[1], resource1, resource2)
        assertItems(result[1], item1, item3)
        assertResources(result[2], resource1, resource2)
        assertItems(result[2], item1, item4)
    }

    def "resource won't work on an item throughout the interruptions' ranges"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 3, 1))
        def capacityCalculator = Mock(CapacityCalculator) {
            calculate(item) >> 1.0f
        }
        def resource = new ProjectResource('resource', capacityCalculator, [new Pair<Float, Float>(0.0f, 1.0f), new Pair<Float, Float>(2.0f, 3.0f)])

        def cut = new TimelineFactory(1, [resource])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 5
        result[0].isEmpty()
        result[1] == [new WorkPackage(resource, item, 1.0f)]
        result[2].isEmpty()
        result[3] == [new WorkPackage(resource, item, 1.0f)]
        result[4] == [new WorkPackage(resource, item, 1.0f)]
    }

    def "assign working resource instead of non-working"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def resource1 = new ProjectResource('resource1', fullCapacityCalculator, [new Pair<Float, Float>(0.0f, 1.0f)])
        def resource2 = new ProjectResource('resource2', fullCapacityCalculator, [])

        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 1
        result[0] == [new WorkPackage(resource2, item, 1.0f)]
    }

    def "shall assign second resource when it's available"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 3, 2))
        def resource1 = new ProjectResource('resource1', fullCapacityCalculator, [])
        def resource2 = new ProjectResource('resource2', fullCapacityCalculator, [new Pair<Float, Float>(0.0f, 1.0f)])

        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 2
        result[0] == [new WorkPackage(resource1, item, 1.0f)]
        result[1] == [new WorkPackage(resource1, item, 1.0f),
                      new WorkPackage(resource2, item, 1.0f)]
    }

    def "shall assign a resource to the next item right away"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 5, 3))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))

        def cut = new TimelineFactory(1, [resource1, resource2, resource3])

        when:
        def result = cut.create([[item1, item2]])

        then:
        result.size() == 2
        result[0] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item1, 1.0f),
                      new WorkPackage(resource3, item1, 1.0f)]
        result[1] == [new WorkPackage(resource1, item1, 1.0f),
                      new WorkPackage(resource2, item1, 1.0f),
                      new WorkPackage(resource3, item2, 1.0f)]
    }

    def "shall focus on items with most impact"() {
        given:
        def item1 = new Item('item1', new Item.Parameters('name', 4, 2))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 1))
        def item3 = new Item('item3', new Item.Parameters('name', 3, 1))
        item3.addDependency(item2)

        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item1, item2], [item3]])

        then:
        result.size() == 4
        assertResources(result[0], resource1, resource2)
        assertItems(result[0], item1, item2)
        assertResources(result[1], resource1, resource2)
        assertItems(result[1], item1, item3)
        assertResources(result[2], resource1, resource2)
        assertItems(result[2], item1, item3)
        assertResources(result[3], resource1, resource2)
        assertItems(result[3], item1, item3)
    }

    def "shall not schedule a package with 0 capacity if no other possibility"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def resource = Mock(ProjectResource) {
            getCapacity(item, 0.0f, _) >> 0.0f
            getCapacity(item, 1.0f, _) >> 0.0f
            getCapacity(item, 2.0f, _) >> 1.0f
        }
        def cut = new TimelineFactory(1, [resource])

        when:
        def result = cut.create([[item]])

        then:
        result.size() == 3
        result[0].isEmpty()
        result[1].isEmpty()
        assertResources(result[2], resource)
        assertItems(result[2], item)
    }

    def "shall throw when no resource available over defined time"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 1, 1))
        def resource = Mock(ProjectResource) {
            getCapacity(item, 0.0f, _) >> 0.0f
            getCapacity(item, 1.0f, _) >> 0.0f
            getCapacity(item, 2.0f, _) >> 0.0f
            getCapacity(item, 3.0f, _) >> 1.0f
        }
        def cut = new TimelineFactory(1, [resource])
        cut.setMaxInactivity(2.0f)

        when:
        cut.create([[item]])

        then:
        thrown(TimelineFactory.ExceededMaxInactivityException)
    }

    def "shall throw ExceededMaxInactivityException when no resource available all the time"() {
        given:
        def item = new Item('item', new Item.Parameters('name', 2, 1))
        def resource = Mock(ProjectResource) {
            getCapacity(item, 0.0f, _) >> 0.0f
            getCapacity(item, 1.0f, _) >> 0.0f
            getCapacity(item, 2.0f, _) >> 1.0f
            getCapacity(item, 3.0f, _) >> 0.0f
            getCapacity(item, 4.0f, _) >> 0.0f
            getCapacity(item, 5.0f, _) >> 1.0f
        }
        def cut = new TimelineFactory(1, [resource])
        cut.setMaxInactivity(2.0f)

        when:
        cut.create([[item]])

        then:
        noExceptionThrown()
    }

    def "shall not assign resource with no capacity"() {
        given:
        def resource1 = Mock(ProjectResource) { getCapacity(_, _, _) >> 0.0f }
        def resource2 = Mock(ProjectResource) { getCapacity(_, _, _) >> 1.0f }
        def item1 = new Item('item1', new Item.Parameters('name', 1, 1))
        def item2 = new Item('item2', new Item.Parameters('name', 1, 2))
        def item3 = new Item('item2', new Item.Parameters('name', 1, 1))
        def cut = new TimelineFactory(1, [resource1, resource2])

        when:
        def result = cut.create([[item1, item2, item3]])

        then:
        result.every { it.size() == 1 && it[0].resource == resource2 }
    }

}
