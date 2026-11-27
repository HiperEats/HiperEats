package pt.ua.hipereats.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Not to be merged. Demonstrates that the SonarCloud quality gate (HE-40) blocks a pull request:
 * this class has no tests, so the coverage of the new code is 0%, below the 80% of "Sonar way".
 * It has no business rule and nothing calls it.
 */
public final class QualityGateDemo {

    private QualityGateDemo() {
    }

    public static String describeBattery(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Battery must be between 0 and 100: " + percent);
        }
        if (percent == 0) {
            return "empty";
        }
        if (percent < 20) {
            return "low";
        }
        if (percent < 80) {
            return "medium";
        }
        return "high";
    }

    public static String formatDistance(double kilometres) {
        if (kilometres < 0) {
            throw new IllegalArgumentException("Distance cannot be negative: " + kilometres);
        }
        if (kilometres < 1) {
            long metres = Math.round(kilometres * 1000);
            return metres + " m";
        }
        return String.format("%.1f km", kilometres);
    }

    public static List<String> describeAll(List<Integer> batteries) {
        List<String> descriptions = new ArrayList<>();
        for (Integer percent : batteries) {
            if (percent == null) {
                descriptions.add("unknown");
            } else {
                descriptions.add(describeBattery(percent));
            }
        }
        return descriptions;
    }

    public static int countLow(List<Integer> batteries) {
        int low = 0;
        for (Integer percent : batteries) {
            if (percent != null && percent < 20) {
                low++;
            }
        }
        return low;
    }
}
