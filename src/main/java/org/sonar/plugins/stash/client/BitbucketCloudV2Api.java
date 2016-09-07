package org.sonar.plugins.stash.client;

import com.ning.http.client.AsyncHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.wickedsource.diffparser.api.DiffParser;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.nio.charset.Charset;
import java.util.List;

public class BitbucketCloudV2Api implements AutoCloseable {
    public static final String DEFAULT_PREFIX = "https://api.bitbucket.org/2.0";

    private String prefix;
    private StashCredentials credentials;
    private int timeout;
    private AsyncHttpClient httpClient;

    public BitbucketCloudV2Api(String prefix, StashCredentials credentials, int timeout, AsyncHttpClient httpClient ) {
        this.prefix = prefix;
        this.credentials = credentials;
        this.timeout = timeout;
        this.httpClient = httpClient;
    }

    public BitbucketCloudV2Api(StashCredentials credentials, int timeout , AsyncHttpClient httpClient) {
        this(DEFAULT_PREFIX, credentials, timeout, httpClient);
    }

    public void close() {
        httpClient.close();
    }

    public StashDiffReport getPullRequestDiffs(String project, String repository, String pullRequestId) {
        String response = getPlain(url("repositories", project, repository, "pullrequests", pullRequestId, "diff"));
        DiffParser diffParser = new UnifiedDiffParser();
        List<Diff> diffs = diffParser.parse(response.getBytes(Charset.forName("UTF-8")));
        StashDiffReport result = new StashDiffReport();
        for (Diff diff: diffs) {
            StashDiff element = new StashDiff(StashPlugin.CONTEXT_ISSUE_TYPE, diff.getFromFileName(), diff.);
            result.add(element);
        }
        return result;
    }

    public String getPlain(String url) {
        return null;
    }

    private String url(String... parts) {
        return prefix + '/' + StringUtils.join(parts, '/');
    }
}
