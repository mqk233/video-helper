package top.mqk233.videohelper.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JSON工具类
 *
 * @author mqk233
 * @since 2021-7-30
 */
public class JsonUtils {
    /**
     * 将数组转换成集合
     *
     * @param jsonArray 要转换的JSON数组
     * @return 转换之后的JSON集合
     */
    public static List<JSONObject> arrayToList(JSONArray jsonArray) {
        return Optional.ofNullable(jsonArray).map(item -> item.stream().map(String::valueOf).map(JSON::parseObject).collect(Collectors.toList())).orElse(new ArrayList<>());
    }
}
