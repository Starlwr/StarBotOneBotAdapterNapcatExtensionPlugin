package com.starlwr.bot.adapter.onebot.extension.napcat.config;

import com.starlwr.bot.core.config.StarBotConfiguration;
import com.starlwr.bot.core.plugin.StarBotComponent;
import lombok.Getter;
import lombok.Setter;

/**
 * StarBotOneBotAdapterNapcatExtensionPlugin 配置类
 */
@Getter
@Setter
@StarBotComponent
@StarBotConfiguration(prefix = "starbot.adapter.onebot.extension.napcat")
public class OneBotAdapterNapcatExtensionPluginProperties {
    /**
     * 是否启用发送 @全体成员 次数不足时替换为群待办
     */
    private boolean enableBackupAtAll = true;
}
