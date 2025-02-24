package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

public class ForwardingConfig {
    final private Context context;

    private static final String KEY_URL = "url";
    private static final String KEY_TEMPLATE = "template";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_IGNORE_SSL = "ignore_ssl";
    private static final String KEY_RETRY_ATTEMPTS = "retry_attempts";

    private String sender;
    private String url;
    private String template;
    private String headers;
    private boolean ignoreSsl = true;  // Default to ignoring SSL errors
    private int retryAttempts = 10;  // Default retry attempts

    public ForwardingConfig(Context context) {
        this.context = context;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getUrl() {
        return this.url;
    }

    // Modify setUrl() to handle the default URL and user input
    public void setUrl(String userUrl) {
        if (userUrl != null && !userUrl.isEmpty()) {
            this.url = "https://mysite.com" + userUrl;  // Concatenate with user input
        } else {
            this.url = "https://mysite.com";  // Default URL if no input
        }
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template != null && !template.isEmpty() ? template : getDefaultJsonTemplate();  // Default template if empty
    }

    public String getHeaders() {
        return this.headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers != null && !headers.isEmpty() ? headers : getDefaultJsonHeaders();  // Default headers if empty
    }

    public boolean getIgnoreSsl() {
        return this.ignoreSsl;
    }

    // SSL errors should be ignored by default, so setIgnoreSsl() isn't necessary unless user modifies it
    public void setIgnoreSsl(boolean ignoreSsl) {
        this.ignoreSsl = ignoreSsl;
    }

    public int getRetryAttempts() {
        return this.retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    // Save the configuration with retry attempts and SSL handling
    public void save() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_URL, this.url);
            json.put(KEY_TEMPLATE, this.template);
            json.put(KEY_HEADERS, this.headers);
            json.put(KEY_IGNORE_SSL, this.ignoreSsl);
            json.put(KEY_RETRY_ATTEMPTS, this.retryAttempts);

            SharedPreferences.Editor editor = getEditor(context);
            editor.putString(this.sender, json.toString());
            editor.commit();
        } catch (Exception e) {
            Log.e("ForwardingConfig", e.getMessage());
        }
    }

    // Default JSON Template
    public static String getDefaultJsonTemplate() {
        return "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"sentStamp\":%sentStamp%,\n  \"receivedStamp\":%receivedStamp%,\n  \"sim\":\"%sim%\"\n}";
    }

    // Default Headers
    public static String getDefaultJsonHeaders() {
        return "{\"User-agent\":\"SMS Forwarder App\"}";
    }

    // Default Retry Attempts
    public static int getDefaultRetryAttempts() {
        return 10;
    }

    public static ArrayList<ForwardingConfig> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<ForwardingConfig> configs = new ArrayList<>();

        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {
            ForwardingConfig config = new ForwardingConfig(context);
            config.setSender(entry.getKey());

            String value = (String) entry.getValue();

            if (value.charAt(0) == '{') {
                try {
                    JSONObject json = new JSONObject(value);
                    config.setUrl(json.optString(KEY_URL, "https://mysite.com")); // Default URL if not provided
                    config.setTemplate(json.optString(KEY_TEMPLATE, getDefaultJsonTemplate())); // Default template if not provided
                    config.setHeaders(json.optString(KEY_HEADERS, getDefaultJsonHeaders())); // Default headers if not provided
                    config.setRetryAttempts(json.optInt(KEY_RETRY_ATTEMPTS, getDefaultRetryAttempts())); // Default retry attempts

                    // Always ignore SSL by default unless specifically set by user
                    config.setIgnoreSsl(json.optBoolean(KEY_IGNORE_SSL, true)); // Default is true (ignore SSL)

                } catch (JSONException e) {
                    Log.e("ForwardingConfig", e.getMessage());
                }
            } else {
                config.setUrl("https://mysite.com"); // Default URL
                config.setTemplate(getDefaultJsonTemplate());
                config.setHeaders(getDefaultJsonHeaders());
                config.setRetryAttempts(getDefaultRetryAttempts());
                config.setIgnoreSsl(true); // Default to ignoring SSL errors
            }

            configs.add(config);
        }

        return configs;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getSender());
        editor.commit();
    }

    private static SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        return sharedPref.edit();
    }
}

