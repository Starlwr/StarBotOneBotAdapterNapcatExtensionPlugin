package com.starlwr.bot.adapter.onebot.extension.napcat.discovery;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.api.OneBotHttpAdapter;
import com.starlwr.bot.adapter.onebot.config.OneBotSender;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NapCat 服务发现切面测试
 * <p>
 * 使用 Mockito 替代 OneBot HTTP 接口，通过真实 Spring AOP 接口代理调用版本查询，
 * 验证服务发现切点能够命中、API 响应原样返回，并将对应推送平台登记至 {@link NapcatServiceHolder}。
 * 测试不发起真实 OneBot HTTP 请求。
 */
class NapCatServiceDiscoveryAspectTest {
    /**
     * 验证版本查询能够识别 NapCat 服务并登记对应推送平台
     * <p>
     * 模拟 app_name 为 NapCat.OneBot 的版本响应，通过 OneBotHttpAdapter 代理调用版本查询，
     * 断言返回同一个响应对象，且服务容器按平台名称保存原始 OneBotSender 实例。
     */
    @Test
    void discoversNapcatThroughOneBotApiProxy() {
        OneBotSender sender = new OneBotSender();
        sender.setName("napcat");
        JSONObject params = new JSONObject();
        JSONObject response = new JSONObject().fluentPut("app_name", "NapCat.OneBot");
        OneBotHttpAdapter target = mock(OneBotHttpAdapter.class);
        when(target.getVersionInfo(sender, params)).thenReturn(response);

        NapcatServiceHolder holder = new NapcatServiceHolder();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setInterfaces(OneBotHttpAdapter.class);
        factory.addAspect(new NapCatServiceDiscoveryAspect(holder));
        OneBotHttpAdapter proxy = factory.getProxy();

        assertSame(response, proxy.getVersionInfo(sender, params));
        assertTrue(holder.isNapcat(sender.getName()));
        assertSame(sender, holder.getNapcat(sender.getName()));
    }
}
