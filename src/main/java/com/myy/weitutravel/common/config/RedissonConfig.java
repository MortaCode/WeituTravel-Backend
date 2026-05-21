package com.myy.weitutravel.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedissonConfig {

    @Value("${redisson.single-server-config.address:redis://127.0.0.1:6379}")
    private String address;

//    @Value("${redisson.single-server-config.password:}")
//    private String password;

    @Value("${redisson.single-server-config.connection-pool-size:64}")
    private int connectionPoolSize;

    @Value("${redisson.single-server-config.connection-minimum-idle-size:10}")
    private int connectionMinimumIdleSize;

    @Value("${redisson.single-server-config.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${redisson.single-server-config.timeout:3000}")
    private int timeout;

    @Value("${redisson.single-server-config.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redisson.single-server-config.retry-interval:1500}")
    private int retryInterval;

    @Value("${redisson.threads:16}")
    private int threads;

    @Value("${redisson.netty-threads:32}")
    private int nettyThreads;

    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单节点配置
        SingleServerConfig singleConfig = config.useSingleServer()
                .setAddress(address)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval);

//        if (password != null && !password.isEmpty()) {
//            singleConfig.setPassword(password);
//        }

        // 设置全局线程池配置
        config.setThreads(threads)
                .setNettyThreads(nettyThreads);

        return Redisson.create(config);
    }
}
