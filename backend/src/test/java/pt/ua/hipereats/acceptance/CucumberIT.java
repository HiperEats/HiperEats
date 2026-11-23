package pt.ua.hipereats.acceptance;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the Gherkin acceptance scenarios in {@code src/test/resources/features} with Failsafe
 * ({@code ./mvnw verify}).
 *
 * <p>Scenarios tagged {@code @wip} describe stories that are not implemented yet and are left out
 * of the build. Whoever implements a story writes its step definitions in this package and removes
 * the tag from its scenarios.
 *
 * <p>The JSON report in {@code target/cucumber} keeps the {@code @HE-n} and {@code @BRn} tags, so
 * the results can be imported into Xray.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pt.ua.hipereats.acceptance")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, json:target/cucumber/cucumber.json, html:target/cucumber/cucumber.html")
class CucumberIT {
}
