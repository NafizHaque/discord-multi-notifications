package com.example;

import java.util.Collections;
import java.util.Collection;


import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import net.runelite.api.VarClientStr;
import net.runelite.api.ScriptID;
import net.runelite.client.game.ItemStack;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.ui.DrawManager;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
	name = "Discord Multi Notifications"
)
public class MultiNotificationsPlugin extends Plugin
{
	private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";
	private boolean notificationStarted;
	@Inject
	private Client client;

	@Inject
	private MultiNotificationsConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private DrawManager drawManager;

	private List<String> lootNpcs;

	private static String itemImageUrl(int itemId)
	{
		return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
	}



	@Override
	protected void startUp()
	{
		lootNpcs = Collections.emptyList();
	}

	@Override
	protected void shutDown()
	{
	}


	@Provides
	MultiNotificationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MultiNotificationsConfig.class);
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			if (player == client.getLocalPlayer())
			{
				sendMessage();
			}
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		NPC npc = npcLootReceived.getNpc();
		Collection<ItemStack> items = npcLootReceived.getItems();

		processLoot(npc.getName(), items);
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived)
	{
		if (lootReceived.getType() != LootRecordType.EVENT && lootReceived.getType() != LootRecordType.PICKPOCKET)
		{
			return;
		}

		processLoot(lootReceived.getName(), lootReceived.getItems());
	}

	private String getPlayerName()
	{
		return client.getLocalPlayer().getName();
	}

	private void processLoot(String name, Collection<ItemStack> items)
	{
		MultiWebhookBody discordWebhookBody = new MultiWebhookBody();

		boolean sendMessage = false;
		StringBuilder stringBuilder = new StringBuilder();
		if (config.includeUsername()) {
			stringBuilder.append("\n**").append(getPlayerName()).append("**").append(":\n\n");
		}
		stringBuilder.append("***").append(name).append("***").append(":\n");
		final int targetValue = config.lootValue();
		for (ItemStack item : items) {
			int itemId = item.getId();
			int qty = item.getQuantity();

			int price = itemManager.getItemPrice(itemId);
			long total = (long) price * qty;

			if (config.includeLowValueItems() || total >= targetValue) {
				sendMessage = true;
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				stringBuilder.append("*").append(qty).append(" x ").append(itemComposition.getName()).append("*");
				if (config.stackValue()) {
					stringBuilder.append(" (").append(QuantityFormatter.quantityToStackSize(total)).append(")");
				}
				stringBuilder.append("\n");
				discordWebhookBody.getEmbeds().add(new MultiWebhookBody.Embed(new MultiWebhookBody.UrlEmbed(itemImageUrl(itemId))));
				System.out.println(stringBuilder.toString());

			}
		}
		String[] webHooksLoots = config.webhookLoot().split(",");
		for( String webhookLoot : webHooksLoots){
			if (sendMessage) {
				discordWebhookBody.setContent(stringBuilder.toString());
				sendWebhook(discordWebhookBody, webhookLoot);
			}
		}
	}


	@Subscribe
	public void onScriptPreFired(ScriptPreFired scriptPreFired)
	{
		MultiWebhookBody discordWebhookBody = new MultiWebhookBody();

		switch (scriptPreFired.getScriptId())
		{
			case ScriptID.NOTIFICATION_START:
				notificationStarted = true;
				break;
			case ScriptID.NOTIFICATION_DELAY:
				if (!notificationStarted)
				{
					return;
				}
				String notificationTopText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
				String notificationBottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);
				if (notificationTopText.equalsIgnoreCase("Collection log") && config.includeCollectionLogItems())
				{
					String entry = Text.removeTags(notificationBottomText).substring("New item:".length());
					String playerName = client.getLocalPlayer().getName();

					String screenshotString = playerName + " just received " + "**" + entry +"**";


					String[] webHooksLoots =  config.webhookLoot().split(",");
					for( String webhookLoot : webHooksLoots){
						discordWebhookBody.setContent(screenshotString);
						sendWebhook(discordWebhookBody, webhookLoot);
					}
				}
				notificationStarted = false;
				break;
		}
	}

	private void sendMessage()
	{
		String playerName = client.getLocalPlayer().getName();
		MultiWebhookBody discordWebhookBody = new MultiWebhookBody();
		String deathString;

		if (config.includeName())
		{
			deathString = String.format("%s %s", playerName, config.deathMessage());
		}
		else
		{
			deathString = config.deathMessage();
		}

		String[] webHooks = config.webhook().split(",");
		for( String webhook : webHooks){

			discordWebhookBody.setContent(deathString);
			sendWebhook(discordWebhookBody, webhook);
		}
	}

	private void sendWebhook(MultiWebhookBody discordWebhookBody, String configUrl)
	{
		if (Strings.isNullOrEmpty(configUrl)) { return; }

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));

		sendWebhookWithScreenshot(url, requestBodyBuilder);
	}

	private void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try
			{
				imageBytes = convertImageToByteArray(bufferedImage);
			}
			catch (IOException e)
			{
				log.warn("Error converting image to byte array", e);
				return;
			}

			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
		});
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
}
