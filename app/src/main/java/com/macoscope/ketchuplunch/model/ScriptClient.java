package com.macoscope.ketchuplunch.model;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.script.model.ExecutionRequest;
import com.google.api.services.script.model.Operation;
import com.macoscope.ketchuplunch.BuildConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Based on https://developers.google.com/apps-script/guides/rest/quickstart/android#step_5_setup_the_sample
 */
public class ScriptClient {

    private com.google.api.services.script.Script script = null;
    private String projectKey = BuildConfig.PROJECT_KEY;
    private String environment = BuildConfig.ENV;

    public ScriptClient(GoogleAccountCredential credential, String rootUrl) {
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        script = new com.google.api.services.script.Script.Builder(
                new NetHttpTransport(), jsonFactory, setHttpTimeout(credential))
                .setApplicationName("Obiaaad for Android")
                .setRootUrl(rootUrl)
                .build();
    }

    public <T> T getDataFromApi(String function, List<Object> parameters) throws IOException, GoogleAuthException {
        ArrayList<Object> params = new ArrayList<>(parameters);
        params.add(getEnvIndex(parameters), environment);
        ExecutionRequest request = new ExecutionRequest().setFunction(function).setParameters(params)
                .setDevMode(false);
        Operation operation = script.scripts().run(projectKey, request).execute();

        if (operation.getResponse() != null &&
                operation.getResponse().get("result") != null) {
            return (T) (operation.getResponse().get("result"));
        } else {
            throw new IOException(getScriptErrorMessage(operation));
        }
    }

    private int getEnvIndex(List<Object> parameters) {
        if (parameters.size() >= 3) {
            return 2;
        } else {
            return 0;
        }
    }

    private String getScriptErrorMessage(Operation op) {
        if (op.getError() == null) {
            return "";
        }
        Map<String, Object> detail = op.getError().getDetails().get(0);
        String errorMessage = (String) detail.get("errorMessage");
        return errorMessage;
    }

    private static HttpRequestInitializer setHttpTimeout(
            final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest)
                    throws java.io.IOException {
                requestInitializer.initialize(httpRequest);
                // This allows the API to call (and avoid timing out on)
                // functions that take up to 6 minutes to complete (the maximum
                // allowed script run time), plus a little overhead.
                httpRequest.setReadTimeout(380000);
            }
        };
    }
}
