package com.starlwr.bot.adapter.onebot.extension.napcat.fallback;

import com.alibaba.fastjson2.JSONObject;
import com.starlwr.bot.adapter.onebot.config.OneBotSender;
import com.starlwr.bot.adapter.onebot.extension.napcat.api.NapcatHttpAdapter;
import com.starlwr.bot.adapter.onebot.extension.napcat.discovery.NapcatServiceHolder;
import com.starlwr.bot.core.message.Message;
import com.starlwr.bot.core.message.PushTargetType;
import com.starlwr.bot.core.message.StarBotMessageSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 全体成员提醒降级切面测试
 * <p>
 * 使用 Mockito 替代 NapCat HTTP 接口与 Core 消息发送器，通过真实 Spring AOP 类代理触发发送切点，
 * 覆盖 @全体成员 不可用时降级为群待办，以及可用时保留原消息发送的两种场景。
 * 发送成功由手动执行消息成功回调模拟，不连接真实机器人或发起 HTTP 请求。
 */
class BackupAtAllAspectTest {
    /**
     * 验证 @全体成员 不可用时移除占位符，并在消息发送成功后创建群待办
     * <p>
     * 模拟 can_at_all 为 false，验证消息移除 @全体成员 占位符后仍交给原发送器处理。
     * 随后设置消息 ID 并执行成功回调，捕获群待办请求，核对目标群号与已发送消息 ID。
     */
    @Test
    void createsGroupTodoAfterSendingWhenAtAllIsUnavailable() {
        OneBotSender sender = new OneBotSender();
        sender.setName("napcat");
        NapcatServiceHolder holder = new NapcatServiceHolder();
        holder.registerNapcat(sender);
        NapcatHttpAdapter http = mock(NapcatHttpAdapter.class);
        when(http.getGroupAtAllRemain(eq(sender), any(JSONObject.class)))
                .thenReturn(new JSONObject().fluentPut("can_at_all", false));
        StarBotMessageSender target = mock(StarBotMessageSender.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new BackupAtAllAspect(holder, http));
        StarBotMessageSender proxy = factory.getProxy();
        Message message = Message.create(sender.getName(), PushTargetType.GROUP, "123", "通知{at=all}").get(0);

        proxy.send(message);

        assertEquals("通知", message.getContent());
        verify(target).send(message);
        // 模拟 Core 在消息发送成功后写入消息 ID 并执行成功回调
        message.setId("message-123");
        message.getOnSuccessCallbacks().forEach(Runnable::run);
        ArgumentCaptor<JSONObject> todoParams = ArgumentCaptor.forClass(JSONObject.class);
        verify(http).setGroupTodo(eq(sender), todoParams.capture());
        assertEquals(123L, todoParams.getValue().get("group_id"));
        ArgumentCaptor<JSONObject> quotaParams = ArgumentCaptor.forClass(JSONObject.class);
        verify(http).getGroupAtAllRemain(eq(sender), quotaParams.capture());
        assertEquals(123L, quotaParams.getValue().get("group_id"));
        assertEquals("message-123", todoParams.getValue().getString("message_id"));
    }

    /**
     * 验证 @全体成员 可用时保留原消息，且不触发群待办降级
     * <p>
     * 模拟 can_at_all 为 true，通过发送器代理发送群消息，验证消息内容保持不变，
     * 原发送器正常收到消息，且未附加成功回调或调用设置群待办接口。
     */
    @Test
    void keepsAtAllWhenItIsAvailable() {
        OneBotSender sender = new OneBotSender();
        sender.setName("napcat");
        NapcatServiceHolder holder = new NapcatServiceHolder();
        holder.registerNapcat(sender);
        NapcatHttpAdapter http = mock(NapcatHttpAdapter.class);
        when(http.getGroupAtAllRemain(eq(sender), any(JSONObject.class)))
                .thenReturn(new JSONObject().fluentPut("can_at_all", true));
        StarBotMessageSender target = mock(StarBotMessageSender.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new BackupAtAllAspect(holder, http));
        StarBotMessageSender proxy = factory.getProxy();
        Message message = Message.create(sender.getName(), PushTargetType.GROUP, "123", "通知{at=all}").get(0);

        proxy.send(message);

        assertEquals("通知{at=all}", message.getContent());
        assertEquals(0, message.getOnSuccessCallbacks().size());
        verify(target).send(message);
        verify(http, never()).setGroupTodo(eq(sender), any(JSONObject.class));
    }
}
