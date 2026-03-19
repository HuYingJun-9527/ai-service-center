package com.hyj.aicodehelper.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyj.aicodehelper.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CityDataService {

    private final RedisUtil redisUtil;

    private static final String REDIS_KEY_PREFIX = "city:code:";

    public CityDataService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 解析test.json文件并将数据存入Redis
     * @return 存储成功的记录数
     */
    @Operation(summary = "解析并存储城市数据", description = "解析test.json文件并将城市数据存储到Redis中")
    public int parseAndStoreCityData() {
        try {
            // 读取JSON文件
            ClassPathResource resource = new ClassPathResource("test.json");
            InputStream inputStream = resource.getInputStream();

            ObjectMapper objectMapper = new ObjectMapper();

            // 解析JSON文件
            Map<String, Object> jsonData = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> cityCodeList = (List<Map<String, Object>>) jsonData.get("城市代码");

            int count = 0;

            // 遍历省份和城市数据
            for (Map<String, Object> provinceData : cityCodeList) {
//                String province = (String) provinceData.get("省");
                List<Map<String, Object>> cities = (List<Map<String, Object>>) provinceData.get("市");

                for (Map<String, Object> city : cities) {
                    String cityName = (String) city.get("市名");
                    String code = (String) city.get("编码");

                    // 以市名为key，编码为value存入Redis
                    String redisKey = REDIS_KEY_PREFIX + cityName;
                    redisUtil.set(redisKey, code);
                    count++;
                }
            }

            return count;

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 根据城市名获取城市编码
     * @param cityName 城市名
     * @return 城市编码，如果不存在返回null
     */
    @Operation(summary = "获取城市编码", description = "根据城市名称获取对应的城市编码")
    public String getCityCode(String cityName) {
        String redisKey = REDIS_KEY_PREFIX + cityName;
        Object code = redisUtil.get(redisKey);
        return code != null ? code.toString() : null;
    }

    /**
     * 获取所有城市编码数据
     * @return 城市名和编码的映射
     */
    @Operation(summary = "获取所有城市编码", description = "获取所有城市名称和编码的映射关系")
    public Map<String, String> getAllCityCodes() {
        // 这里可以使用Redis的keys命令获取所有相关key，但生产环境建议使用scan
        // 为了简单演示，我们重新解析文件并返回所有数据
        try {
            ClassPathResource resource = new ClassPathResource("test.json");
            InputStream inputStream = resource.getInputStream();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonData = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> cityCodeList = (List<Map<String, Object>>) jsonData.get("城市代码");

            Map<String, String> result = new HashMap<>();

            for (Map<String, Object> provinceData : cityCodeList) {
                List<Map<String, Object>> cities = (List<Map<String, Object>>) provinceData.get("市");

                for (Map<String, Object> city : cities) {
                    String cityName = (String) city.get("市名");
                    String code = (String) city.get("编码");
                    result.put(cityName, code);
                }
            }

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 删除指定城市的编码数据
     * @param cityName 城市名
     * @return 是否删除成功
     */
    @Operation(summary = "删除城市编码", description = "删除指定城市的编码数据")
    public boolean deleteCityCode(String cityName) {
        String redisKey = REDIS_KEY_PREFIX + cityName;
        redisUtil.del(redisKey);
        return true;
    }

    /**
     * 更新城市编码
     * @param cityName 城市名
     * @param newCode 新的编码
     * @return 是否更新成功
     */
    @Operation(summary = "更新城市编码", description = "更新指定城市的编码")
    public boolean updateCityCode(String cityName, String newCode) {
        String redisKey = REDIS_KEY_PREFIX + cityName;
        return redisUtil.set(redisKey, newCode);
    }
}