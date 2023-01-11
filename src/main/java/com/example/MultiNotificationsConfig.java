package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


@ConfigGroup("discordMultinotifications")
public interface MultiNotificationsConfig extends Config
{


	@ConfigSection(
			name = "Death Notifications",
			description = "This handles Deaths and fires yor death image off to as many servers as you please",
			position = 0
	)
	String deathSection = "deathSection";

	@ConfigItem(
			keyName = "deathWebhook",
			name = "Death URL" + "\n" + " (split URLs with a comma)",
			description = "Have many discord Webhook URLS split with a comma.",
			section = deathSection,
			position = 0
	)
	String webhook();

	@ConfigItem(
			keyName = "deathMessage",
			name = "Death message",
			description = "The message that will be included with the screenshot",
			section = deathSection,
			position = 1
	)
	default String deathMessage() {
			return "has passed away.";
	}
	@ConfigItem(
			keyName = "includeName",
			name = "Include Player Name",
			description = "Include player name at the start of the death notification message",
			section = deathSection,
			position = 2
	)
	default boolean includeName() {
		return true;
	}

	@ConfigSection(
			name = "Loot Notification",
			description = "This handles Deaths and fires yor death image off to as many servers as you please",
			position = 99
	)
	String lootSection = "lootSection";

	@ConfigItem(
			keyName = "webhook",
			name = "Loot URL" + "\n" + " (split URLs with a comma)",
			description = "The Discord Webhook URL to send messages to",
			section = lootSection,
			position = 0
	)
	String webhookLoot();


	@ConfigItem(
			keyName = "sendScreenshot",
			name = "Send Screenshot",
			description = "Includes a screenshot when receiving the loot",
			section = lootSection,
			position = 1
	)
	default boolean sendScreenshot()
	{
		return false;
	}

	@ConfigItem(
			keyName = "includeLowValueItems",
			name = "Minimum Value of Loot to send",
			description = "Only log loot items worth more than the value set in loot value option.",
			section = lootSection,
			position = 2
	)
	default boolean includeLowValueItems()
	{
		return false;
	}

	@ConfigItem(
			keyName = "lootvalue",
			name = "Loot Value",
			description = "Only logs loot worth more then the given value. 0 to disable.",
			section = lootSection,
			position = 3
	)
	default int lootValue()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "stackvalue",
			name = "Include Stack Value",
			description = "Include the value of each stack.",
			section = lootSection,
			position = 4
	)
	default boolean stackValue()
	{
		return false;
	}

	@ConfigItem(
			keyName = "collectionLogItem",
			name = "Include collection log items",
			description = "Configures whether a message will be automatically sent to discord when you obtain a new collection log item.",
			position = 5,
			section = lootSection
	)
	default boolean includeCollectionLogItems()
	{
		return true;
	}
	@ConfigItem(
			keyName = "includeusername",
			name = "Include Player Name",
			description = "Include player name at the start of the loot notification message",
			section = lootSection,
			position = 6
	)
	default boolean includeUsername()
	{
		return false;
	}



}



