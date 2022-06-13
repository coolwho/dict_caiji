import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class SoGouDict {
    public static void main(String[] args) throws Exception {
        //item-simple
        ChromeDriver chromeDriver = createChromeDriver();
        List<String> dicts = DictUtil.readDict("Level4luan_2.json");
        ChromeStringFetch chromeStringFetch = new ChromeStringFetch();
        int count = 0;
        for (String dict : dicts) {
            ++count;
            File dictFile = new File("sogou/" + dict + ".html");

            System.out.println("正在采集第" + count + "/" + dicts.size() + "个单词：" + dict);
            if (dictFile.exists()) {
                System.out.println("已经存在。");
                continue;
            }
            long start = System.currentTimeMillis();
            for (; ; ) {
                chromeDriver.get("http://fanyi.sogou.com/text?keyword=" + dict);
                String dictHtml = chromeStringFetch.fetchUrl(chromeDriver, (reqRrl, params) -> {
                    if (reqRrl.contains("keyword=" + dict)) {
                        return true;
                    }
                    return false;
                });
                if (dictHtml == null || !dictHtml.contains("trans-main") || !dictHtml.contains(dict)) {
                    System.out.println("采集失败，正在重试");
                    chromeDriver.quit();
                    chromeDriver = createChromeDriver();
                } else {
                    System.out.println("采集成功，耗时：" + (System.currentTimeMillis() - start)+"ms");
                    FileUtils.writeStringToFile(dictFile, dictHtml, "utf-8");
                    break;
                }
            }
        }
//
//        WebElement element = chromeDriver.findElement(By.cssSelector(".main-left"));
//        System.out.println(element.getAttribute("innerHTML"));
        chromeDriver.quit();
    }

    static ChromeDriver createChromeDriver() {
        DesiredCapabilities cap = DesiredCapabilities.chrome();
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        cap.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        ChromeDriver chromeDriver = new ChromeDriver(cap);
        return chromeDriver;
    }
}
