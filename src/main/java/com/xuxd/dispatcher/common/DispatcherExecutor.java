package com.xuxd.dispatcher.common;

import com.xuxd.dispatcher.beans.DingResponse;
import com.xuxd.dispatcher.utils.SignUtil;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * webhook-dingtalk-dispatcher.
 *
 * @author xuxd
 * @date 2021-12-02 19:17:10
 **/
@Component
@Slf4j
public class DispatcherExecutor {

    @Autowired
    private RestClient restClient;

    private static final String SIGN = "sign";

    private static final String TIMESTAMP = "timestamp";

    @Async
    public void executeAsync(Map<String, Object> args, String body, String url, String secret, boolean keysFilter,
        FilterType type,
        String... keys) {
        execute(args, body, url, secret, keysFilter, type, keys);
    }

    public DingResponse execute(Map<String, Object> args, String body, String url, String secret, boolean keysFilter,
        FilterType type,
        String... keys) {
        // 是否通过一些关键字进行过滤
        if (keysFilter) {
            boolean pass = false;
            switch (type) {
                case OR:
                    for (String key : keys) {
                        if (StringUtils.isNotBlank(key) && body.indexOf(key.trim()) >= 0) {
                            pass = true;
                            break;
                        }
                    }
                    break;
                case AND:
                    int size = keys.length;
                    if (size > 0) {
                        for (String key : keys) {
                            if (StringUtils.isNotBlank(key) && body.indexOf(key.trim()) >= 0) {
                                size--;
                            }
                        }
                        pass = size == 0;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("filter type is illegal.");
            }

            if (!pass) {
                DingResponse response = new DingResponse();
                response.setErrcode(-9999);
                response.setErrmsg("keys is not match.");
                return response;
            }
        }

        // trie树比上面的写法慢
//        if (!new Trie(keys).matchAny(body)) {
//            return;
//        }

        String suffix = "&timestamp={timestamp}";

        try {
            // 为每个机器人生成独立的时间戳，避免时间戳共享导致签名失败
            String timestamp = String.valueOf(System.currentTimeMillis());
            if (!args.containsKey(TIMESTAMP)) {
                args.put(TIMESTAMP, timestamp);
            } else {
                // 如果args中已有时间戳，创建一个新的副本以避免修改原始args
                Map<String, Object> newArgs = new HashMap<>(args);
                newArgs.put(TIMESTAMP, timestamp);
                args = newArgs;
            }
            if (StringUtils.isNotBlank(secret)) {
                suffix = "&sign={sign}&timestamp={timestamp}";
                String sign = SignUtil.sign(secret, (String) args.get(TIMESTAMP));
                args.put(SIGN, sign);
            }
            DingResponse response = restClient.post(url + suffix, body, DingResponse.class, args);
            if (response.getErrcode() != 0) {
                log.error("url: {}, response: {}", url, response);
            } else {
                log.info("url: {}, response: {}", url, response);
            }
            return response;
        } catch (Exception e) {
            log.error("execute error.", e);
            DingResponse response = new DingResponse();
            response.setErrcode(-9999);
            response.setErrmsg("unknown error: " + e.getMessage());
            return response;
        }
    }
}
