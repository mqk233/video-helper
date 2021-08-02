package top.mqk233.videohelper.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.mqk233.videohelper.exception.ServiceException;

import javax.annotation.PostConstruct;

/**
 * 雪花算法ID生成器
 *
 * @author mqk233
 * @since 2021-8-1
 */
@Slf4j
@Component
public class SnowFlake {
    private final long START_TIMESTAMP = 1609430400000L;
    private final long DATACENTER_ID_BITS = 5L;
    private final long WORKER_ID_BITS = 5L;
    private final long SEQUENCE_BITS = 12L;
    private final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private final long DATACENTER_ID_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private final long WORKER_ID_LEFT_SHIFT = SEQUENCE_BITS;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @Value("${snowflake.worker-id:0}")
    private long workerId;

    @Value("${snowflake.datacenter-id:0}")
    private long datacenterId;

    @PostConstruct
    private void initSnowFlake() {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new ServiceException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new ServiceException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        log.info("Snowflake worker starting. worker Id: {} datacenter Id: {}", workerId, datacenterId);
    }

    /**
     * 获取下一个ID
     *
     * @return 下一个ID
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp < lastTimestamp) {
            throw new ServiceException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - currentTimestamp));
        }
        if (lastTimestamp == currentTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                long timestamp = System.currentTimeMillis();
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
                currentTimestamp = timestamp;
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = currentTimestamp;
        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT) | (datacenterId << DATACENTER_ID_LEFT_SHIFT) | (workerId << WORKER_ID_LEFT_SHIFT) | sequence;
    }
}
