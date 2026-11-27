package pt.ua.hipereats.xray;

import app.getxray.xray.junit.customjunitxml.DefaultXrayTestMetadataReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Reads the Xray metadata of each test for the JUnit XML report that CI imports into Xray (HE-41).
 *
 * <p>A test covers the stories named in its {@code @Requirement}, as by default, and also every tag
 * that is a story key. Cucumber passes the Gherkin tags of a scenario, its feature's included, as
 * JUnit tags without the "@", so a scenario of a feature tagged {@code @HE-29} covers HE-29 with no
 * change to the feature files.
 *
 * <p>Set in {@code xray-junit-extensions.properties} ({@code test_metadata_reader}).
 */
public class StoryTagMetadataReader extends DefaultXrayTestMetadataReader {

    /** A key of the Jira project HE: "HE-29", not "he-29" or "HE-29a". */
    private static final Pattern STORY_KEY = Pattern.compile("HE-\\d+");

    @Override
    public List<String> getRequirements(TestIdentifier test) {
        Set<String> stories = new LinkedHashSet<>(super.getRequirements(test));
        test.getTags().stream()
                .map(TestTag::getName)
                .filter(tag -> STORY_KEY.matcher(tag).matches())
                .forEach(stories::add);
        return List.copyOf(stories);
    }
}
