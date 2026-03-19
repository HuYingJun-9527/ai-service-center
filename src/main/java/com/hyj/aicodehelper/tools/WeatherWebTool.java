package com.hyj.aicodehelper.tools;

import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WeatherWebTool {

    private final RedisUtil redisUtil;

    private static final String REDIS_KEY_PREFIX = "city:code:";

    @Autowired
    public WeatherWebTool(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public WeatherWebTool() {
        this.redisUtil = null;
    }

    /**
     * 获取指定城市的当前天气
     * @param city 城市名称（中文）
     */
    @Tool(name = "getCurrentWeather", value = """
            Retrieves current weather information for Chinese cities from weather.com.cn.
            Use this tool when the user asks about current weather, temperature, humidity, wind, 
            or air quality in any Chinese city.
            Input should be a Chinese city name (e.g., "北京", "上海", "广州", "深圳").
            """
    )
    public String getCurrentWeather(@P(value = "the Chinese city name") String city) {
        try {
            // 步骤1: 获取城市代码
            String cityCode = getCityCode(city);
            if (cityCode == null) {
                return "未找到城市: " + city + "，请确认城市名称是否正确（如：北京、上海、杭州）";
            }

            // 步骤2: 获取天气页面
            String weatherUrl = "http://www.weather.com.cn/weather/" + cityCode + ".shtml";

            Document weatherDoc = Jsoup.connect(weatherUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .get();

            // 步骤3: 解析当前天气（今天第一个时段的天气）
            Element todayWeatherDiv = weatherDoc.selectFirst(".today .clearfix");
            if (todayWeatherDiv == null) {
                return "无法解析" + city + "的天气页面结构";
            }

            // 获取天气信息
            Element weatherWea = todayWeatherDiv.selectFirst(".wea");
            Element weatherTem = todayWeatherDiv.selectFirst(".tem");
            Element weatherWin = todayWeatherDiv.selectFirst(".win");
            Element weatherHumidityDiv = weatherDoc.selectFirst(".shidu");

            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("=== ").append(city).append(" 当前天气 ===\n");
            result.append("更新时间：").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n");

            if (weatherWea != null) {
                result.append("天气状况：").append(weatherWea.text()).append("\n");
            }

            if (weatherTem != null) {
                // 提取温度数字
                String tempText = weatherTem.text();
                // 移除span标签
                tempText = tempText.replaceAll("<span>|</span>", "");
                // 提取数字
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(tempText);
                List<String> temps = new ArrayList<>();
                while (matcher.find()) {
                    temps.add(matcher.group());
                }

                if (!temps.isEmpty()) {
                    if (temps.size() >= 2) {
                        result.append("温度：").append(temps.get(0)).append("°C ~ ").append(temps.get(1)).append("°C\n");
                    } else {
                        result.append("温度：").append(temps.get(0)).append("°C\n");
                    }
                } else {
                    result.append("温度：").append(tempText.replace("℃", "°C")).append("\n");
                }
            }

            if (weatherWin != null) {
                result.append("风力风向：").append(weatherWin.text()).append("\n");
            }

            if (weatherHumidityDiv != null) {
                Elements humidityItems = weatherHumidityDiv.select("b");
                for (Element item : humidityItems) {
                    String text = item.text();
                    result.append(text).append(" ");
                }
                result.append("\n");
            }

            // 步骤4: 尝试获取实时温度（从天气数据接口）
            try {
                String realtimeUrl = "http://d1.weather.com.cn/sk_2d/" + cityCode + ".html?_=" + System.currentTimeMillis();
                Document realtimeDoc = Jsoup.connect(realtimeUrl)
                        .header("Referer", "http://www.weather.com.cn/weather/" + cityCode + ".shtml")
                        .ignoreContentType(true)
                        .timeout(10000)
                        .get();

                String jsonData = realtimeDoc.text();
                // 解析JSON数据
                if (jsonData.contains("dataSK")) {
                    String tempMatch = extractJsonValue(jsonData, "temp");
                    String sdMatch = extractJsonValue(jsonData, "SD");
                    String wdMatch = extractJsonValue(jsonData, "WD");
                    String wsMatch = extractJsonValue(jsonData, "WS");
                    String weatherMatch = extractJsonValue(jsonData, "weather");

                    result.append("\n【实时数据】\n");
                    if (tempMatch != null) {
                        result.append("实时温度：").append(tempMatch).append("°C\n");
                    }
                    if (sdMatch != null) {
                        result.append("相对湿度：").append(sdMatch).append("%\n");
                    }
                    if (wdMatch != null && wsMatch != null) {
                        result.append("风向风速：").append(wdMatch).append(" ").append(wsMatch).append("\n");
                    }
                    if (weatherMatch != null) {
                        result.append("当前天气：").append(weatherMatch).append("\n");
                    }
                }
            } catch (Exception e) {
                log.debug("获取实时天气数据失败: {}", e.getMessage());
            }

            // 步骤5: 尝试获取空气质量
            try {
                String airUrl = "http://www.weather.com.cn/air/" + cityCode + ".shtml";
                Document airDoc = Jsoup.connect(airUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(8000)
                        .get();

                Element airQuality = airDoc.selectFirst(".polldata em");
                if (airQuality != null) {
                    result.append("空气质量：").append(airQuality.text()).append("\n");
                }

                // 尝试获取更多空气质量信息
                Elements airItems = airDoc.select(".detail-air li");
                if (!airItems.isEmpty()) {
                    for (Element item : airItems) {
                        String text = item.text().trim();
                        if (!text.isEmpty()) {
                            result.append(text).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("获取空气质量信息失败: {}", e.getMessage());
            }

            return result.toString();

        } catch (IOException e) {
            log.error("获取天气信息失败", e);
            return "获取天气信息时出现网络错误: " + e.getMessage();
        } catch (Exception e) {
            log.error("解析天气信息失败", e);
            return "解析天气信息失败: " + e.getMessage();
        }
    }

    /**
     * 获取指定城市的未来一周天气预报
     * @param city 城市名称（中文）
     */
    @Tool(name = "getWeeklyWeatherForecast", value = """
            Retrieves 7-day weather forecast for Chinese cities from weather.com.cn.
            Use this tool when the user asks about future weather, weekly forecast, 
            tomorrow's weather, or multi-day forecasts in Chinese cities.
            Input should be a Chinese city name.
            """
    )
    public String getWeeklyWeatherForecast(@P(value = "the Chinese city name") String city) {
        try {
            // 步骤1: 获取城市代码
            String cityCode = getCityCode(city);
            if (cityCode == null) {
                return "未找到城市: " + city;
            }

            // 步骤2: 获取7天天气预报页面
            String forecastUrl = "http://www.weather.com.cn/weather/" + cityCode + ".shtml";

            Document forecastDoc = Jsoup.connect(forecastUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get();

            // 步骤3: 解析7天天气预报
            Elements forecastItems = forecastDoc.select(".t.clearfix li");
            if (forecastItems.isEmpty()) {
                forecastItems = forecastDoc.select(".c7d .clearfix li");
            }

            if (forecastItems.isEmpty()) {
                return "无法获取" + city + "的天气预报信息";
            }

            // 获取当前日期
            LocalDate today = LocalDate.now();

            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("=== ").append(city).append(" 未来一周天气预报 ===\n");
            result.append("更新日期：").append(today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n\n");

            int itemCount = Math.min(forecastItems.size(), 7); // 最多显示7天

            for (int i = 0; i < itemCount; i++) {
                Element item = forecastItems.get(i);

                // 获取日期
                String dateText = "";
                Element dateElement = item.selectFirst("h1");
                if (dateElement != null) {
                    dateText = dateElement.text();
                } else {
                    // 如果没有找到h1标签，尝试其他选择器
                    Element dayElement = item.selectFirst(".date");
                    if (dayElement != null) {
                        dateText = dayElement.text();
                    } else {
                        // 如果没有日期信息，使用推算的日期
                        LocalDate forecastDate = today.plusDays(i);
                        String dayOfWeek = forecastDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
                        dateText = forecastDate.getMonthValue() + "月" + forecastDate.getDayOfMonth() + "日(" + dayOfWeek + ")";
                    }
                }

                // 获取天气信息
                String weatherDay = "", weatherNight = "", tempDay = "", tempNight = "", windDay = "", windNight = "";

                // 尝试获取白天天气
                Element weaElement = item.selectFirst(".wea");
                if (weaElement != null) {
                    weatherDay = weaElement.text();
                }

                // 尝试获取夜间天气
                Element weaNightElement = item.selectFirst(".wea_n");
                if (weaNightElement != null) {
                    weatherNight = weaNightElement.text();
                }

                // 获取温度
                Element temElement = item.selectFirst(".tem");
                if (temElement != null) {
                    String tempText = temElement.text();
                    // 提取最高温和最低温
                    Pattern pattern = Pattern.compile("(\\d+)");
                    Matcher matcher = pattern.matcher(tempText);
                    List<String> temps = new ArrayList<>();
                    while (matcher.find()) {
                        temps.add(matcher.group());
                    }

                    if (temps.size() >= 2) {
                        tempDay = temps.get(0); // 最高温
                        tempNight = temps.get(1); // 最低温
                    } else if (!temps.isEmpty()) {
                        tempDay = temps.getFirst();
                    }
                }

                // 获取风力
                Element winElement = item.selectFirst(".win");
                if (winElement != null) {
                    windDay = winElement.text();
                }

                Element winNightElement = item.selectFirst(".win_n");
                if (winNightElement != null) {
                    windNight = winNightElement.text();
                }

                // 构建每天的信息
                result.append("【").append(dateText).append("】\n");

                if (!weatherDay.isEmpty() && !weatherNight.isEmpty() && !weatherDay.equals(weatherNight)) {
                    result.append("天气：白天").append(weatherDay).append("，夜间").append(weatherNight).append("\n");
                } else if (!weatherDay.isEmpty()) {
                    result.append("天气：").append(weatherDay).append("\n");
                }

                if (!tempDay.isEmpty() && !tempNight.isEmpty()) {
                    result.append("温度：").append(tempNight).append("°C ~ ").append(tempDay).append("°C\n");
                } else if (!tempDay.isEmpty()) {
                    result.append("温度：").append(tempDay).append("°C\n");
                }

                if (!windDay.isEmpty() && !windNight.isEmpty() && !windDay.equals(windNight)) {
                    result.append("风力：白天").append(windDay).append("，夜间").append(windNight).append("\n");
                } else if (!windDay.isEmpty()) {
                    result.append("风力：").append(windDay).append("\n");
                }

                // 如果是今天，添加特别标识
                if (i == 0) {
                    result.append("（今天）");
                } else if (i == 1) {
                    result.append("（明天）");
                } else if (i == 2) {
                    result.append("（后天）");
                }

                result.append("\n\n");
            }

            // 添加温馨提示
            result.append("温馨提示：天气预报仅供参考，请以实际天气为准。\n");

            return result.toString();

        } catch (IOException e) {
            log.error("获取天气预报失败", e);
            return "获取天气预报时出现网络错误: " + e.getMessage();
        } catch (Exception e) {
            log.error("解析天气预报失败", e);
            return "解析天气预报失败: " + e.getMessage();
        }
    }
   /* public static void main(String[] args) {
        WeatherWebTool weatherWebTool = new WeatherWebTool();// 测试当前天气
        String weather = weatherWebTool.getCurrentWeather("南京");
        System.out.println(weather);

// 测试一周预报
        String forecast = weatherWebTool.getWeeklyWeatherForecast("南京");
        System.out.println(forecast);

// 测试天气指数
        String index = weatherWebTool.getWeatherIndex("南京");
        System.out.println(index);
    }*/


    /**
     * 根据城市名称获取城市代码
     * @param city 城市名称
     * @return 城市代码
     */
    private String getCityCode(String city) {
        // 优先从Redis缓存中获取
        try {
            String redisKey = REDIS_KEY_PREFIX + city.trim();
            String code = null;
            if (redisUtil != null) {
                code = ObjectUtils.isEmpty(redisUtil.get(redisKey)) ? null : redisUtil.get(redisKey).toString();
            }
            if (code != null) {
                return code;
            }
            // 如果本地没有，可以返回null或尝试其他后备方案
            log.warn("核心映射中未找到城市代码: {}", city);
            return null;
        } catch (Exception e) {
            log.error("获取城市代码失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据城市名称获取城市代码
     */
   /* private String getCityCoded(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String searchUrl = "http://toy1.weather.com.cn/search?cityname=" + encodedCity;

            Document searchDoc = Jsoup.connect(searchUrl)
                    .ignoreContentType(true)
                    .timeout(10000)
                    .get();

            String searchResult = searchDoc.text();

            if (searchResult.isEmpty() || searchResult.equals("[]")) {
                return null;
            }

            // 解析搜索结果，格式如：["北京~101010100","顺义~101010400","海淀~101010200"]
            searchResult = searchResult.substring(1, searchResult.length() - 1); // 移除方括号

            // 分割多个结果
            String[] cityResults = searchResult.split(",");
            if (cityResults.length == 0) {
                return null;
            }

            // 取第一个结果（最匹配的）
            String firstResult = cityResults[0].replace("\"", "");
            String[] parts = firstResult.split("~");

            if (parts.length >= 2) {
                return parts[1];
            }

            return null;

        } catch (Exception e) {
            log.error("获取城市代码失败: {}", e.getMessage());
            return null;
        }
    }*/

    /**
     * 从JSON字符串中提取指定字段的值
     * @param json JSON字符串
     * @param key 要提取的字段名
     * @return 字段值，或null如果未找到
     */
    private String extractJsonValue(String json, String key) {
        try {
            Pattern pattern = Pattern.compile("\"" + key + "\":\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }

            pattern = Pattern.compile("\"" + key + "\":([^,}\\]]*)");
            matcher = pattern.matcher(json);
            if (matcher.find()) {
                String value = matcher.group(1).replace("\"", "").trim();
                return value.isEmpty() ? null : value;
            }
        } catch (Exception e) {
            log.debug("提取JSON值失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取天气指数和生活建议（增强功能）
     * @param city 城市名称（中文）
     */
    @Tool(name = "getWeatherIndex", value = """
            Retrieves weather indices and life suggestions for Chinese cities.
            Use this tool when the user asks about UV index, dressing suggestions,
            air quality, or lifestyle recommendations based on weather.
            Input should be a Chinese city name.
            """
    )
    public String getWeatherIndex(@P(value = "the Chinese city name") String city) {
        try {
            String cityCode = getCityCode(city);
            if (cityCode == null) {
                return "未找到城市: " + city;
            }

            String indexUrl = "http://www.weather.com.cn/weather/" + cityCode + ".shtml";

            Document indexDoc = Jsoup.connect(indexUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            StringBuilder result = new StringBuilder();
            result.append("=== ").append(city).append(" 天气指数与生活建议 ===\n\n");

            // 尝试获取指数信息
            Elements indexItems = indexDoc.select(".live_index li");
            if (!indexItems.isEmpty()) {
                for (Element item : indexItems) {
                    Element indexName = item.selectFirst(".name");
                    Element indexValue = item.selectFirst(".level");
                    Element indexDesc = item.selectFirst(".txt");

                    if (indexName != null) {
                        result.append("【").append(indexName.text()).append("】");
                        if (indexValue != null) {
                            result.append(" ").append(indexValue.text());
                        }
                        result.append("\n");
                        if (indexDesc != null) {
                            result.append("建议：").append(indexDesc.text()).append("\n");
                        }
                        result.append("\n");
                    }
                }
            } else {
                result.append("暂无天气指数信息\n");
            }

            // 添加通用建议
            result.append("温馨提示：\n");
            result.append("1. 夏季注意防晒防暑，冬季注意保暖\n");
            result.append("2. 雨天出行请携带雨具\n");
            result.append("3. 雾霾天气减少户外活动\n");
            result.append("4. 关注实时天气变化，合理安排出行\n");

            return result.toString();

        } catch (Exception e) {
            log.error("获取天气指数失败", e);
            return "获取天气指数失败: " + e.getMessage();
        }
    }
}