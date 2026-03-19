package com.hyj.aicodehelper.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StreamStopManager {
    
    private final Map<String, Integer> streamIdToMemoryId = new ConcurrentHashMap<>();
    private final Map<Integer, Sinks.Many<String>> memoryIdToSink = new ConcurrentHashMap<>();
    
    /**
     * 注册流
     */
    public void registerStream(String streamId, int memoryId) {
        streamIdToMemoryId.put(streamId, memoryId);
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        memoryIdToSink.put(memoryId, sink);
        log.info("注册流: streamId={}, memoryId={}", streamId, memoryId);
    }
    
    /**
     * 注销流
     */
    public void unregisterStream(String streamId) {
        Integer memoryId = streamIdToMemoryId.remove(streamId);
        if (memoryId != null) {
            memoryIdToSink.remove(memoryId);
            log.info("注销流: streamId={}, memoryId={}", streamId, memoryId);
        }
    }
    
    /**
     * 停止指定memoryId的流
     */
    public boolean stopStream(int memoryId) {
        Sinks.Many<String> sink = memoryIdToSink.get(memoryId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("停止流: memoryId={}", memoryId);
            return true;
        }
        log.warn("未找到对应的流: memoryId={}", memoryId);
        return false;
    }


    public Sinks.Many<String> getStopSink(int memoryId) {
        return memoryIdToSink.get(memoryId);
    }
}