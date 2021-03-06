# Testing HyGene
Current project status:
[![Build Status](https://travis-ci.com/nielsdebruin/dna.svg?token=MPR2aq1yzRi2MdAzgtdk&branch=master)](https://travis-ci.com/nielsdebruin/dna)
[![CodeCov](https://codecov.io/gh/nielsdebruin/dna/branch/master/graph/badge.svg?token=iCcqwI3I98)](https://codecov.io/gh/nielsdebruin/dna)

## Abstract
This document describes special test cases and the test progress. The special test cases are usually cases that could not be well tested due to various reasons. We explain them to show that they were not 'forgotten', but untested for a reason. Also, we give more insight in our testing process in the chapter about testing progress. We gave a per sprint overview of the testing related activities and possible difficulties that we encountered during that sprint.

## Special Components

### JavaFX UI
For our UI we have set up end-to-end/acceptance tests that verify the working of the functionality in our UI. There are some exception when the testing tool, `TestFX`, cannot control the UI, for instance opening a file through the file opener. Tests for these parts of the application are handled differently.


## Testing Tools
Automated testing is considered vital to writing a successful application by our team. We are using modern test tools such as JUnit 5, AssertJ and Mockito to write intuitive tests. Whenever possible, we try to write tests at multiple levels (unit, integration, end-to-end, acceptance). To this end we are using TestFX to test our UI on a mix between end-to-end and acceptance level.

We are using Jacoco in our Gradle builds to generate code coverage reports. These reports include line coverage, which will be used for grading purposes. However, internally we like to use Codecov's metric, which takes both line and branch coverage into account.

## Coverage Metric
The [Codecov metric]((http://docs.codecov.io/docs/frequently-asked-questions)) is defined as follows: `round((hits / (hits + partials + misses)) * 100, 5)`. This based on both line and branch coverage.

## Progress
In this chapter we will the describe our testing progress throughout the sprints. The test coverage will be expressed in the percentage of lines that are covered by tests.

### Sprint 1
In the first sprint we started testing immediately from the start of the project. In fact, JUnit 5 and AssertJ were one of the first tools we set up. Without GraphStream we reached a total amount of line coverage of 87% (CodeCov 78%). Since GraphStream is only present temporarily to show a first prototype, and will be taken out of our code base, we have not paid too much attention to testing our GraphStream integration (it will be taken out of the code base before the end of the next sprint).  Therefore, with GraphStream included we reached a total amount of line coverage of 76% (CodeCov 70%).

## Sprint 2
As mentioned in the section about 'Sprint 1', during this sprint we have removed our GraphStream integration and replaced this with drawing primitives ourselves. Drawing primitives cannot be 'well tested' in the sense that we cannot test if something is actually displayed. The other features such as the new data structure and recent files are well tested.
We reached a total amount of line coverage of 83% (CodeCov 80%).

## Sprint 3
In this sprint we worked on porting to a new data structure. Also, we have changed/improved our UI a lot, especially our graph rendering. As mentioned in 'Sprint 2', this works with the drawing of primitives which is hard to test. 

For the database implementation we had to deal with some tests that were interfering each other, and therefore were not completely isolated. The issue was hard to track down, but is fixed now.

For our GFA file parser we have added some integration tests, especially for parsing metadata. This improves our confidence in our parsing pipeline.

Therefore, our coverage of this week decreased a little to 81% (CodeCov 77%).

## Sprint 4
In this sprint we worked on improving our UI tests. We refactored the the UI classes to make them more testable.

We also made sure that every new part/feature we added was well tested and did not run into any special problems.

Therefore, our coverage of this week improved to 82% (CodeCov 78%).

## Sprint 5
This week we took a huge hit from removing old code (in terms of coverage). Because we completely rewrote our datastructure / parsing pipeline we still had the old versions around. All this code was well tested (same holds for the new versions of course), but that did unfortunately mean that after removing it, there was (in total) less code covered. Although we did not want to focus too much on improving this, we did our best to improve this by testing the new features well. In our opinion it does not make sense to add tests with only the goal to improve coverage. That would result in many meaningless tests that just 'literally' test that the lines of code were entered correctly instead of that the behavior of the code under test is correct. 

The UI remains hard to test and we're working on improving our UI tests with a JavaFX end-to-end testing framework, which should allow us to test actual interaction with the UI better. Setting it up is, however, rather painful.

Our coverage of this week decreased a little to 79% (CodeCov 76%).

## Sprint 6
This week we tested the new code we added to the project. There are not many comments to be made about our testing process. 

Due to time constraints we did add some thing that *could* have been tested better, but we will make sure to improve the tests where necessary over the coming weeks.

Our coverage of this week increased to 80% (CodeCov 77%).

## Sprint 7
During this sprint we worked a lot on adding new features at a high pace. Unfortunately, this was not really good for our code quality and test coverage. During the next sprints we will work hard on increasing this again.

Our coverage of this week decreased to 79% (CodeCov 76%).

## Sprint 8
This last sprint was a continuation of last week in terms of testing. At the beginning of the week we added missing tests for the features introduct last sprint. However, we had to add a lot of new features at a high pace, again, which was not necessarily good in terms of covered code.

Our coverage of this week decreased to 78% (CodeCov 75%).
