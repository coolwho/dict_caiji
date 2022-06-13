import com.alibaba.fastjson.JSONPath;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class DictUtil {
    static List<String> readDict(String file) throws Exception {
        File wordFile = new File(file);
        List<String> wordList = FileUtils.readLines(wordFile, "utf8");
        return wordList.stream().map(e -> {
            String dict = JSONPath.eval(e, "$.headWord").toString();
            return dict;
        }).collect(Collectors.toList());

    }

}
