package com.starlwr.bot.adapter.onebot.extension.napcat.api;

import com.starlwr.bot.core.plugin.StarBotComponent;
import com.starlwr.bot.core.support.http.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Proxy;

/**
 * Napcat HTTP 扩展服务注册器
 */
@Slf4j
@StarBotComponent
public class NapcatHttpAdapterRegistrar {
    @Bean
    public NapcatHttpAdapterProxy napcatHttpAdapterProxy(HttpService http) {
        return new NapcatHttpAdapterProxy(http);
    }

    @Bean
    public NapcatHttpAdapter napcatHttpAdapter(NapcatHttpAdapterProxy proxy) {
        return (NapcatHttpAdapter) Proxy.newProxyInstance(
                NapcatHttpAdapter.class.getClassLoader(),
                new Class[]{NapcatHttpAdapter.class},
                proxy
        );
    }
}
