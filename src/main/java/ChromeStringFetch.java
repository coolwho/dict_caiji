import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ChromeStringFetch {
    public interface RequestMatch {
        boolean match(String reqUrl, JSONObject params);
    }

    private final HashSet<String> requestIdSet = new HashSet<>();
    private int retryCount = 30;
    private int delayTime = 500;
    private int idleTime = 100;

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String fetchUrl(ChromeDriver chromeDriver, RequestMatch requestMatch) throws InterruptedException {
        String ans = null;
        Thread.sleep(delayTime);
        Loop:
        for (int i = 0; i < retryCount; i++) {
            try {
                Logs logs = chromeDriver.manage().logs();
                LogEntries performance = logs.get("performance");
                for (LogEntry logEntry : performance) {
                    JSONObject jsonObject = JSON.parseObject(logEntry.getMessage());
                    JSONObject message = jsonObject.getJSONObject("message");
                    String method = message.getString("method");
                    if ("Network.responseReceived".equals(method)) {
                        JSONObject resp = message.getJSONObject("params").getJSONObject("response");
                        if (requestMatch.match(resp.getString("url"), resp.getJSONObject("params"))) {
                            String requestId = message.getJSONObject("params").getString("requestId");
                            if (requestIdSet.contains(requestId)) {
                                throw new Exception("continue");
                            }
                            requestIdSet.add(requestId);
                            CommandExecutor commandExecutor = chromeDriver.getCommandExecutor();
                            if (commandExecutor instanceof HttpCommandExecutor) {
                                Field field = HttpCommandExecutor.class.getDeclaredField("nameToUrl");
                                field.setAccessible(true);
                                Map<String, CommandInfo> map = (Map<String, CommandInfo>) field.get(commandExecutor);
                                if (!map.containsKey("Network.getResponseBody")) {
                                    HashMap<String, CommandInfo> other = new HashMap<>(map);
                                    other.put("Network.getResponseBody", new CommandInfo("/session/:sessionId/goog/cdp/execute",
                                            HttpVerb.POST));
                                    field.set(commandExecutor, other);
                                }
                            }
                            SessionId sessionId = chromeDriver.getSessionId();
                            JSONObject cmdparams = new JSONObject();
                            Command command = new Command(sessionId,
                                    "Network.getResponseBody", new HashMap<String, Object>() {{
                                cmdparams.put("requestId", requestId);
                                put("cmd", "Network.getResponseBody");
                                put("params", cmdparams);
                            }});
                            Response responseBody = commandExecutor.execute(command);
                            Object body = JSONPath.eval(responseBody.getValue(), "$.body");
                            if (body != null) {
                                ans = body.toString();
                                break Loop;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Thread.sleep(idleTime);
        }

        return ans;
    }
}
