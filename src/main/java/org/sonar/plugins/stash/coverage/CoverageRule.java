package org.sonar.plugins.stash.coverage;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class CoverageRule implements RulesDefinition {
    private Languages languages;
    private static String repositoryName = "coverageEvolution";
    private static String decreasingLineCoverageRule = "decreasingLineCoverage";

    public CoverageRule(Languages languages) {
        this.languages = languages;
    }

    private static String getRepositoryName(String language) {
        return repositoryName + "-" + language;
    }

    @Override
    public void define(Context context) {
        for (Language language : languages.all()) {
            NewRepository repo = context.createRepository(getRepositoryName(language.getKey()), language.getKey());
            repo.setName("Coverage evolution");
            repo.createRule(decreasingLineCoverageRule)
                    .setName("Coverage on files should not decrease")
                    .setMarkdownDescription("Reports if the coverage on a file has decreased.")
                    .setTags("bad-practice")
                    .setSeverity(Severity.BLOCKER)
            ;

            repo.done();
        }
    }

    public static RuleKey decreasingLineCoverageRule(String language) {
        return RuleKey.of(getRepositoryName(language), decreasingLineCoverageRule);
    }

    public static boolean isDecreasingLineCoverage(Issue issue) {
        return isDecreasingLineCoverage(issue.ruleKey());
    }

    public static boolean isDecreasingLineCoverage(RuleKey rule) {
        return rule.repository().startsWith(repositoryName + "-");
    }

    public static boolean isDecreasingLineCoverage(String rule) {
        return isDecreasingLineCoverage(RuleKey.parse(rule));
    }

    public static boolean shouldExecute(ActiveRules rules) {
        return rules.findAll().stream()
                .filter(r -> r.ruleKey().rule().equals(decreasingLineCoverageRule))
                .findFirst().isPresent();
    }
}
