import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

///统计词根
public class GRE {

    public static void main(String[] args) throws IOException {
        File[] dicts = new File("dicts").listFiles();
        if (dicts == null) {
            return;
        }
        int count = 0;
        int noneCount = 0;
        for (File dict : dicts) {
            String dictStr = FileUtils.readFileToString(dict, "utf8");
            int memory_skill = dictStr.indexOf("memory_skill");
            if (memory_skill != -1) {
                count++;
                System.out.println(memory_skill);
                int end = dictStr.indexOf("\"", memory_skill + 15);
                System.out.println(dictStr.substring(memory_skill+15, end));
            } else {
                noneCount++;
                System.out.println(dict.getName());
            }
        }
        System.out.println("有词根：" + count + " 无词根：" + (noneCount) + " 总共：" + (count + noneCount));
    }
}
