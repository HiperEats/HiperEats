package pt.ua.hipereats.acceptance;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the Gherkin acceptance scenarios in {@code src/test/resources/features} with Failsafe
 * ({@code ./mvnw verify}).
 *
 * <p>Scenarios tagged {@code @wip} describe stories that are not implemented yet and are left out
 * of the build. Whoever implements a story writes its step definitions in this package and removes
 * the tag from its scenarios. {@code @ExcludeTags} leaves them out of the test plan itself, so they
 * are not reported as skipped either: Xray would show their story as not run (HE-41).
 *
 * <p>Each scenario reaches Xray through the JUnit XML report of {@code target/xray}, linked to the
 * story of its feature tag ({@code @HE-n}); see {@code pt.ua.hipereats.xray.StoryTagMetadataReader}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ExcludeTags("wip")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pt.ua.hipereats.acceptance")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, json:target/cucumber/cucumber.json, html:target/cucumber/cucumber.html")
class CucumberIT {
}
