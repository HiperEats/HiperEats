package pt.ua.hipereats.xray;

import static org.assertj.core.api.Assertions.assertThat;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

class StoryTagMetadataReaderTest {

    private final StoryTagMetadataReader reader = new StoryTagMetadataReader();

    /** A test as the JUnit Platform reports it, with the given tags (Cucumber passes the Gherkin tags). */
    private static TestIdentifier testWith(TestSource source, String... tags) {
        Set<TestTag> testTags = Arrays.stream(tags).map(TestTag::create)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        AbstractTestDescriptor descriptor = new AbstractTestDescriptor(
                UniqueId.forEngine("test").append("test", "one"), "one", source) {
            @Override
            public Type getType() {
                return Type.TEST;
            }

            @Override
            public Set<TestTag> getTags() {
                return testTags;
            }
        };
        return TestIdentifier.from(descriptor);
    }

    @Test
    void whenScenarioIsTaggedWithAStoryKey_thenItCoversThatStory() {
        TestIdentifier scenario = testWith(null, "HE-29", "BR13");

        assertThat(reader.getRequirements(scenario)).containsExactly("HE-29");
    }

    @Test
    void whenTestHasNoStoryTag_thenItCoversNoStory() {
        TestIdentifier scenario = testWith(null, "BR13", "wip");

        assertThat(reader.getRequirements(scenario)).isEmpty();
    }

    @Test
    void whenTagOnlyLooksLikeAStoryKey_thenItIsIgnored() {
        TestIdentifier scenario = testWith(null, "HE-", "XHE-29", "HE-29a", "he-29");

        assertThat(reader.getRequirements(scenario)).isEmpty();
    }

    @Test
    void whenJUnitTestHasRequirementAndStoryTag_thenItCoversEachStoryOnce() throws Exception {
        Method method = AnnotatedTests.class.getDeclaredMethod("registersScooter");
        TestIdentifier test = testWith(MethodSource.from(AnnotatedTests.class, method), "HE-29", "HE-30");

        assertThat(reader.getRequirements(test)).containsExactlyInAnyOrder("HE-29", "HE-30");
    }

    /** A JUnit test linked to its story with {@code @Requirement}, as the story tests are written. */
    static class AnnotatedTests {

        @Requirement("HE-29")
        void registersScooter() {
            // only its annotation is read
        }
    }
}
