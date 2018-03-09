package scheduler

import spock.lang.Specification

class BoardTest extends Specification {

    def params = new Item.Parameters('name', 1, 1)

    def item1 = new Item('id1', params)
    def item2 = new Item('id2', params)
    def item3 = new Item('id3', params)
    def multithreadItem = new Item('multithreadItem', new Item.Parameters('name', 1, 2))

    def resource1 = Mock(ProjectResource)
    def resource2 = Mock(ProjectResource)
    def resource3 = Mock(ProjectResource)
    def resource4 = Mock(ProjectResource)
    def resource5 = Mock(ProjectResource)

    def "one item"() {
        given:
        def cut = new Board([item1], [])
        def allocs = cut.getTodo()

        when:
        def result = cut.getIndependentTodo()

        then:
        result == [allocs[0]]
    }

    def "shall throw when allocated with unknown allocation"() {
        given:
        def cut = new Board([], [resource1])

        when:
        cut.allocate(new Board.Allocation(item1), resource1)

        then:
        thrown(Board.InvalidAllocationException)
    }

    def "shall throw when allocated with unknown resource"() {
        given:
        def cut = new Board([item1], [])
        def allocs = cut.getTodo()

        when:
        cut.allocate(allocs[0], resource1)

        then:
        thrown(Board.UnknownResourceException)
    }

    def "shall throw when deallocated with unknown allocation"() {
        given:
        def cut = new Board([], [resource1])

        when:
        cut.deallocate(new Board.Allocation(item1), resource1)

        then:
        thrown(Board.InvalidAllocationException)
    }

    def "shall throw when deallocated with non-allocated allocation"() {
        given:
        def cut = new Board([item1], [resource1])
        def allocs = cut.getTodo()

        when:
        cut.deallocate(allocs[0], resource1)

        then:
        thrown(Board.InvalidAllocationException)
    }

    def "shall throw when deallocated with unknown resource"() {
        given:
        def cut = new Board([item1], [])
        def allocs = cut.getTodo()

        when:
        cut.deallocate(allocs[0], resource1)

        then:
        thrown(Board.UnknownResourceException)
    }

    def "shall assign resource"() {
        given:
        def cut = new Board([item1], [resource1])
        def alloc = cut.getTodo()[0]

        when:
        cut.allocate(alloc, resource1)

        then:
        alloc.assignedResources == [resource1]
        cut.getOccupations()[resource1].get() == alloc
    }

    def "shall unassign resource"() {
        given:
        def cut = new Board([item1], [resource1])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        cut.deallocate(alloc, resource1)

        then:
        alloc.assignedResources.isEmpty()
        !cut.getOccupations()[resource1].isPresent()
    }

    def "shall not return ongoing allocs"() {
        given:
        def cut = new Board([item1, item2, item3], [resource1])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        def result = cut.getIndependentTodo()

        then:
        result.size() == 2
        !result.contains(alloc)
    }

    def "shall return only allocs independent from currently ongoing"() {
        given:
        item2.addDependency(item1)
        def cut = new Board([item1, item2, item3], [resource1])
        def alloc1 = cut.getTodo()[0]
        def alloc3 = cut.getTodo()[2]
        cut.allocate(alloc1, resource1)

        when:
        def result = cut.getIndependentTodo()

        then:
        result == [alloc3]
    }

    def "shall return only allocs independent from still 'todo'"() {
        given:
        item2.addDependency(item1)
        def cut = new Board([item1, item2, item3], [resource1])
        def allocs = cut.getTodo()

        when:
        def result = cut.getIndependentTodo()

        then:
        result == [allocs[0], allocs[2]]
    }

    def "shall return deallocated allocs"() {
        given:
        def cut = new Board([item1], [resource1])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)
        cut.deallocate(alloc, resource1)

        when:
        def result = cut.getIndependentTodo()

        then:
        result == [alloc]
    }

    def "shall not return any work done on non-allocated alloc"() {
        given:
        def cut = new Board([item1], [resource1])

        when:
        def workDone = cut.increaseTime(1.0f)

        then:
        cut.getTodo()[0].todo == params.size
        workDone.isEmpty()
    }

    def "shall return a list of work done by an allocated alloc"() {
        given:
        def params = new Item.Parameters('name', size, 1)
        def item = new Item('id', params)

        and:
        def cut = new Board([item], [resource1])
        cut.time = time
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        def workDone = cut.increaseTime(delta)

        then:
        1 * resource1.getCapacity(item, time, delta) >> capacity
        cut.getOngoing()[0].todo == expected
        workDone == [new WorkPackage(resource1, item, capacity)]

        where:
        size | capacity | time | delta | expected
        2    | 1        | 1    | 1     | 1
        7    | 2        | 2    | 3     | 5
        3    | 1        | 3    | 5     | 2
        10   | 3        | 4    | 7     | 7
        5    | 0        | 7    | 6     | 5
    }

    def "shall return work done by all resources assigned to an item"() {
        given:
        def item = new Item('id', new Item.Parameters('name', 1.0f, 3))
        def cut = new Board([item], [resource1, resource2, resource3])

        and:
        resource1.getCapacity(item, _, _) >> 1.0f
        resource2.getCapacity(item, _, _) >> 0.0f
        resource3.getCapacity(item, _, _) >> 2.0f

        and:
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)
        cut.allocate(alloc, resource2)
        cut.allocate(alloc, resource3)

        when:
        def workDone = cut.increaseTime(1.0f)

        then:
        workDone.size() == 3
        workDone.contains(new WorkPackage(resource1, item, 1.0f))
        workDone.contains(new WorkPackage(resource2, item, 0.0f))
        workDone.contains(new WorkPackage(resource3, item, 2.0f))
    }

    def "shall return work done to all items"() {
        given:
        def cut = new Board([item1, item2], [resource1, resource2])

        and:
        resource1.getCapacity(item1, _, _) >> 1.0f
        resource2.getCapacity(item2, _, _) >> 2.0f

        and:
        def alloc1 = cut.getTodo()[0]
        def alloc2 = cut.getTodo()[1]
        cut.allocate(alloc1, resource1)
        cut.allocate(alloc2, resource2)

        when:
        def workDone = cut.increaseTime(1.0f)

        then:
        workDone.size() == 2
        workDone.contains(new WorkPackage(resource1, item1, 1.0f))
        workDone.contains(new WorkPackage(resource2, item2, 2.0f))
    }

    def "shall increase time"() {
        given:
        def cut = new Board([], [])

        when:
        cut.increaseTime(0.5f)
        cut.increaseTime(1.5f)
        cut.increaseTime(0.25f)

        then:
        cut.getTime() == 2.25f
    }

    def "shall throw when invalid delta given"() {
        given:
        def cut = new Board([], [])

        when:
        cut.increaseTime(delta)

        then:
        thrown(expected)

        where:
        delta | expected
        0     | Board.InvalidDeltaException
        -1    | Board.InvalidDeltaException
    }

    def "shall move finished alloc to done and unassign resource"() {
        given:
        def params = new Item.Parameters('name', size, 1)
        def item = new Item('id', params)
        def cut = new Board([item], [resource1])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)
        resource1.getCapacity(item, _, _) >> capacity

        when:
        cut.increaseTime(10)

        then:
        cut.getTodo().isEmpty()
        cut.getOngoing().isEmpty()
        cut.getDone() == [alloc]
        alloc.assignedResources.isEmpty()
        !cut.getOccupations()[resource1].isPresent()

        where:
        size | capacity
        1f   | 1f
        1f   | 2f
    }

    def "shall not remove from ongoing if there is still a resource assigned"() {
        given:
        def cut = new Board([multithreadItem], [resource1, resource2])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)
        cut.allocate(alloc, resource2)

        when:
        cut.deallocate(alloc, resource1)

        then:
        cut.getOngoing().contains(alloc)
        !cut.getOccupations()[resource1].isPresent()
        cut.getOccupations()[resource2].isPresent()
        alloc.assignedResources == [resource2]
    }

    def "shall throw when allocating more than possible"() {
        given:
        def cut = new Board([item1], [resource1, resource2])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        cut.allocate(alloc, resource2)

        then:
        thrown(Board.OverflowingAllocationException)
    }

    def "shall transition to 'ongoing' when assigning a new resource"() {
        given:
        def cut = new Board([item1], [resource1])
        def alloc = cut.getTodo()[0]

        when:
        cut.allocate(alloc, resource1)

        then:
        cut.getTodo().isEmpty()
        cut.getOngoing() == [alloc]
        cut.getDone().isEmpty()
        cut.getOccupations()[resource1].get() == alloc
        alloc.assignedResources == [resource1]
    }

    def "shall transition back in the front of 'todo' when deallocating the last resource"() {
        given:
        def cut = new Board([multithreadItem, item1, item2], [resource1, resource2])
        def alloc1 = cut.getTodo()[0]
        def alloc2 = cut.getTodo()[1]
        def alloc3 = cut.getTodo()[2]
        cut.allocate(alloc1, resource1)
        cut.allocate(alloc1, resource2)

        when:
        cut.deallocate(alloc1, resource1)
        cut.deallocate(alloc1, resource2)

        then:
        cut.getTodo().toList() == [alloc1, alloc2, alloc3]
        cut.getOngoing().isEmpty()
        cut.getDone().isEmpty()
        !cut.getOccupations()[resource1].isPresent()
        !cut.getOccupations()[resource2].isPresent()
        alloc1.assignedResources.isEmpty()
    }

    def "shall not transition when still ongoing"() {
        given:
        def cut = new Board([multithreadItem], [resource1, resource2])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)
        cut.allocate(alloc, resource2)

        when:
        cut.deallocate(alloc, resource1)

        then:
        cut.getTodo().isEmpty()
        cut.getOngoing() == [alloc]
        cut.getDone().isEmpty()
        cut.getOccupations()[resource2].get() == alloc
        !cut.getOccupations()[resource1].isPresent()
        alloc.assignedResources == [resource2]
    }

    def "shall throw then the same resource is beign allocated"() {
        given:
        def cut = new Board([multithreadItem], [resource1])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        cut.allocate(alloc, resource1)

        then:
        thrown(Board.ResourceAlreadyAssignedException)
    }

    def "shall return ongoing allocs with spare threads"() {
        given:
        def cut = new Board([item1, multithreadItem], [resource1, resource2])
        def alloc1 = cut.getTodo()[0]
        def alloc2 = cut.getTodo()[1]
        cut.allocate(alloc1, resource1)
        cut.allocate(alloc2, resource2)

        when:
        def result = cut.getSpareOngoing()

        then:
        result == [alloc2]
    }

    def "shall return free resources able to complete the item (capacity > 0) sorted by capacity"() {
        given:
        def delta = 0.7f
        def cut = new Board([item1], [resource1, resource2, resource3, resource4, resource5])
        def alloc = cut.getTodo()[0]
        cut.allocate(alloc, resource1)

        when:
        def result = cut.getSortedFreeResources(item1, delta)

        then:
        (1.._) * resource2.getCapacity(item1, cut.getTime(), delta) >> 0.5f
        (1.._) * resource3.getCapacity(item1, cut.getTime(), delta) >> 1.5f
        (1.._) * resource4.getCapacity(item1, cut.getTime(), delta) >> 1.0f
        (1.._) * resource5.getCapacity(item1, cut.getTime(), delta) >> 0.0f
        result == [resource3, resource4, resource2]
    }
}
