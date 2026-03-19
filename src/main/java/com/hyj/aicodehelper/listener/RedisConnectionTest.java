package com.hyj.aicodehelper.listener;//package com.hyj.aicodehelper.listener;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class RedisConnectionTest {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    public RedisConnectionTest(RedisTemplate<String, Object> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    /**
//     * 测试Redis连接是否正常
//     * @return 连接测试结果
//     */
//    public String testConnection() {
//        try {
//            // 尝试设置一个测试键值对
//            String testKey = "connection_test";
//            String testValue = "Redis connection is working!";
//
//            redisTemplate.opsForValue().set(testKey, testValue);
//
//            // 读取测试值
//            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
//
//            if (testValue.equals(retrievedValue)) {
//                // 清理测试数据
//                redisTemplate.delete(testKey);
//                return "Redis连接测试成功！";
//            } else {
//                return "Redis连接测试失败：读取的值不匹配";
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            return "Redis连接测试失败：" + e.getMessage();
//        }
//    }
//
//    /**
//     * 获取Redis服务器信息
//     * @return Redis服务器信息
//     */
//    public String getRedisInfo() {
//        try {
//            // 获取Redis连接信息
//            String info = redisTemplate.getConnectionFactory().getConnection().info().toString();
//            return "Redis服务器信息：" + info;
//        } catch (Exception e) {
//            return "无法获取Redis服务器信息：" + e.getMessage();
//        }
//    }
//}