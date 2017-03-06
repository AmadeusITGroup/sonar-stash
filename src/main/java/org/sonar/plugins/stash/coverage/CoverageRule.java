package org.sonar.plugins.stash.coverage;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
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
                    .setName("Decreasing coverage on files")
                    .setMarkdownDescription("Reports if the coverage on a file has decreased.")
            ;

            repo.done();
        }
    }

    public static RuleKey decreasingLineCoverageRule(String language) {
        return RuleKey.of(getRepositoryName(language), decreasingLineCoverageRule);
    }

    public static boolean isDecreasingLineCoverageRule(RuleKey rule) {
        return rule.repository().startsWith(repositoryName + "-");
    }
}
