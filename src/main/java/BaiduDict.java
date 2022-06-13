import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.*;
import org.openqa.selenium.remote.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BaiduDict {
    static class Word {
        public String word;
        public String cigen;
    }

    static List<Word> readWord(String file) throws Exception {
        File wordFile = new File(file);
        List<String> wordList = FileUtils.readLines(wordFile, "utf8");
        return wordList.stream().map(e -> {
            String dict = JSONPath.eval(e, "$.headWord").toString();
            Word word = new Word();
            word.word = dict;
            return word;
        }).collect(Collectors.toList());

    }

    public static void main(String[] args) throws Exception {
        List<Word> words = readWord("Level4luan_2.json");
        ChromeDriver chromeDriver = createChromeDriver();
        int count = 0;
        for (Word word : words) {
            ++count;

            File dictFile = new File("dicts/" + word.word + ".json");
            System.out.println("正在采集第" + (count) + "个单词：" + word.word);
            if (dictFile.exists()) {
                System.out.println("已经存在此数据，跳过");
                continue;
            }
            for (; ; ) {
                long start = System.currentTimeMillis();
                String source = fetchDict(word.word, chromeDriver);
                long end = System.currentTimeMillis();

                if (source != null) {
                    FileUtils.write(dictFile, source, "utf8");
                    System.out.println("采集成功，耗时：" + (end - start) + "ms");
                    break;
                } else {
                    System.out.println("采集失败,尝试重试");
                }

            }

            Thread.sleep(500);
        }
        chromeDriver.quit();

    }

    static ChromeDriver createChromeDriver() {
        DesiredCapabilities cap = DesiredCapabilities.chrome();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        cap.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        return new ChromeDriver(cap);
    }


    static HashSet<String> requestIdMap = new HashSet<>();

    private static String fetchDict(String dict, ChromeDriver chromeDriver) throws InterruptedException {
        String ans = null;
        chromeDriver.get("https://fanyi.baidu.com/#en/zh/" + dict);
        Thread.sleep(500);
        Loop:
        for (int i = 0; i < 30; i++) {
            try {
                Logs logs = chromeDriver.manage().logs();
                LogEntries performance = logs.get("performance");
                for (LogEntry logEntry : performance) {
                    JSONObject jsonObject = JSON.parseObject(logEntry.getMessage());
                    JSONObject message = jsonObject.getJSONObject("message");
                    String method = message.getString("method");
                    if ("Network.responseReceived".equals(method)) {
                        JSONObject resp = message.getJSONObject("params").getJSONObject("response");
                        if (resp.getString("url").contains("v2transapi")) {
                            String requestId = message.getJSONObject("params").getString("requestId");
                            if (requestIdMap.contains(requestId)) {
                                throw new Exception("continue");
                            }
                            requestIdMap.add(requestId);
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

            Thread.sleep(100);
        }
        if (ans != null) {
            if (!ans.contains("trans_result")) {
                return null;
            }
            ans = JSON.parseObject(ans).toJSONString();
        }
        return ans;
    }


}
