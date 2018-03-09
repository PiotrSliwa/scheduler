package scheduler

import spock.lang.Specification

import static spock.util.matcher.HamcrestMatchers.closeTo


class ProjectLengthCostCalculatorTest extends Specification {

    WorkPackage createWorkPackage(ProjectResource resource) {
        return new WorkPackage(resource, Mock(Item), 0.0f)
    }

    def "cost is all resources throughout the whole project"() {
        given:
        def resource1 = Mock(ProjectResource)
        def resource2 = Mock(ProjectResource)
        def resource3 = Mock(ProjectResource)
        def cut = new ProjectLengthCostCalculator()
        def timeline = [
                [createWorkPackage(resource1), createWorkPackage(resource2), createWorkPackage(resource3)],
                [createWorkPackage(resource1), createWorkPackage(resource2), createWorkPackage(resource3)],
                []
        ]

        when:
        def result = cut.calculate(timeline, resolution)

        then:
        result closeTo(expectedResult, 0.0001f)

        where:
        resolution | expectedResult
        1.0f       | 9.0f
        0.5f       | 4.5f
    }

}
