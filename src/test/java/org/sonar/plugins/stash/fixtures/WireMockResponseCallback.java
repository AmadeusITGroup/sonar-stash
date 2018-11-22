package org.sonar.plugins.stash.fixtures;


import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

public class WireMockResponseCallback {
  public interface ResponseTransformerCallback {
    Response transform(Request request, Response response, FileSource files, Parameters parameters);
  }

  private static final String WIREMOCK_EXTENSION_NAME = "foo";
  private static final String CALLBACK_PARAMETER_NAME = "callback-xxx";

  private static final Extension transformer = new DummySQResponseTransformer();

  public static Extension getExtension() {
    return transformer;
  }

  public ResponseDefinitionBuilder callback(ResponseDefinitionBuilder rdb, ResponseTransformerCallback callback) {
    return rdb.withTransformer(
        WIREMOCK_EXTENSION_NAME, CALLBACK_PARAMETER_NAME, new ToStringWrapper(callback)
    );
  }

  private static class DummySQResponseTransformer extends ResponseTransformer {

    @Override
    public String getName() {
      return WIREMOCK_EXTENSION_NAME;
    }

    @Override
    public Response transform(Request request, Response response, FileSource files,
        Parameters parameters) {
      if (parameters == null) {
        return response;
      }
      Object callback = parameters.get(CALLBACK_PARAMETER_NAME);
      if (callback == null) {
        return response;
      }
      if (!(callback instanceof ResponseTransformerCallback)) {
        throw new IllegalStateException();
      }
      return ((ResponseTransformerCallback) callback).transform(request, response, files, parameters);
    }
  }

  private static class ToStringWrapper implements ResponseTransformerCallback {
    private ResponseTransformerCallback wrapped;

    private ToStringWrapper(
        ResponseTransformerCallback wrapped) {

      this.wrapped = wrapped;
    }

    public String getLamba() {
      return wrapped.getClass().getName();
    }

    @Override
    public Response transform(Request request, Response response, FileSource files,
        Parameters parameters) {
      return wrapped.transform(request, response, files, parameters);
    }
  }
}
