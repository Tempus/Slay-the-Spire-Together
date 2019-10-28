package chronospeed;

import com.evacipated.cardcrawl.modthespire.lib.*;

import basemod.*;
import basemod.abstracts.*;
import basemod.interfaces.*;

import org.apache.logging.log4j.*;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import chronospeed.*;

import java.util.*;
import java.lang.*;
import java.nio.*;

import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.*;
import com.megacrit.cardcrawl.integrations.steam.*;

public class NetworkHelper {

	public static SteamMatchmaking matcher;
	public static SteamFriends friends;
	public static SteamNetworking net;
	public static SteamUtils utils;

	public static SteamID id;

	public static ArrayList<SteamID> players = new ArrayList();

	public static int channel = 0;
    private static final Logger logger = LogManager.getLogger("Network Data");


	public void NetworkHelper() {}

	public static void initialize() {
		SteamApps steamApps = (SteamApps)ReflectionHacks.getPrivate(CardCrawlGame.publisherIntegration, SteamIntegration.class, "steamApps");

        matcher = new SteamMatchmaking(new SMCallback());
        net = new SteamNetworking(new SNCallback(), SteamNetworking.API.Client);
        utils = new SteamUtils(new SUtilsCallback());
        friends = new SteamFriends(new SFCallback());

		id = steamApps.getAppOwner();
	}

    @SpirePatch(clz=CardCrawlGame.class, method="update")
    public static class SteamUpdate
    {
        public static void Postfix(CardCrawlGame __instance)
        {
        	NetworkHelper.update();
        }
    }

	// Check every frame for incoming packets.
	public static void update() {
		boolean noPackets = true;
		int bufferSize;
		ByteBuffer data = ByteBuffer.allocateDirect(0); 
		SteamID steamID = new SteamID();

		while (noPackets) {
			bufferSize = net.isP2PPacketAvailable(NetworkHelper.channel);
			if (data.capacity() != bufferSize) {
				data = ByteBuffer.allocateDirect(bufferSize);
			}

			if (bufferSize != 0) {
				logger.info("A packet is available of size " + bufferSize);
				try {
					net.readP2PPacket(steamID, data, NetworkHelper.channel);
					parseData(data, steamID);
				}
				catch (SteamException e) {
					logger.info("Reading the packet failed: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				noPackets = false;
			}
		}
	}

	public static void parseData(ByteBuffer data, SteamID player) {

		for (RemotePlayerWidget playerInfo : TopPanelPlayerPanels.playerWidgets) {
			if (playerInfo.steamUser.getAccountID() == player.getAccountID()) {
				dataType type = dataType.values()[data.getInt()];

				switch (type) {
					// case NetworkHelper.dataType.Rules:
					// 	data.getChar(1, );
					// 	break;
					case Start:
						int start = data.getInt(4);
						logger.info("Start Run: " + start);
						break;
					// case NetworkHelper.dataType.Ready:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.Version:
					// 	data.getChar(1, );
					// 	break;
					case Floor:
						int floorNum = data.getInt(4);
						playerInfo.floor = floorNum;
						logger.info("Floor: " + floorNum);
						break;
					case Hp:
						int Hp = data.getInt(4);
						playerInfo.hp = Hp;
						logger.info("Player HP: " + Hp);
						break;
					case Money:
						int Money = data.getInt(4);
						playerInfo.gold = Money;
						logger.info("Gold: " + Money);
						break;
					// case NetworkHelper.dataType.BossRelic:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.Finish:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.TransferCard:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.TransferRelic:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.EmptyRoom:
					// 	data.getChar(1, );
					// 	break;
					// case NetworkHelper.dataType.BossChosen:
					// 	data.getChar(1, );
					// 	break;
				}
			}
		}
	}

    public static enum dataType
    {
      	Rules, Start, Ready, Version, Floor, Hp, Money, BossRelic, Finish, TransferCard, TransferRelic, EmptyRoom, BossChosen;
      
    	private dataType() {}
    }
		/* My basic data structure will be
			byte - type
			message

			Where message could be:
				Rules - Byte, Byte, Long (char, asc, seed)
				Start - long (timestamp)
				Ready - Byte (boolean)
				Version - ???

				Floor - Short
				HP - Short
				Money - Short
				Boss Relic - Short
				
				Finish - long (timestamp)

				Transfer card
				Transfer relic
				Empty room
				Boss Chosen
		*/


	public static void sendData(NetworkHelper.dataType type) {
		ByteBuffer data = NetworkHelper.generateData(type);	

		for (SteamID player:  NetworkHelper.players) {
			try {
				boolean sent = net.sendP2PPacket(player, data, SteamNetworking.P2PSend.Reliable, NetworkHelper.channel);
				logger.info("SteamID is valid: " + player.isValid());
				logger.info("Packet of type " + type.toString() + " to " + player.getAccountID() + " was " + sent);
			} catch (SteamException e) {
				logger.info("Sending the packet of type " + type.toString() + " failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static ByteBuffer generateData(NetworkHelper.dataType type) {
		ByteBuffer data;

		switch (type) {
			// case NetworkHelper.dataType.Rules:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			case Start:
				data = ByteBuffer.allocateDirect(8);
				data.putInt(4, 1);
				break;
			// case NetworkHelper.dataType.Ready:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.Version:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			case Floor:
				data = ByteBuffer.allocateDirect(8);
				data.putInt(4, AbstractDungeon.floorNum);
				break;
			case Hp:
				data = ByteBuffer.allocateDirect(8);
				data.putInt(4, AbstractDungeon.player.currentHealth);
				break;
			case Money:
				data = ByteBuffer.allocateDirect(8);
				data.putInt(4, AbstractDungeon.player.gold);
				break;
			// case NetworkHelper.dataType.BossRelic:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.Finish:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.TransferCard:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.TransferRelic:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.EmptyRoom:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			// case NetworkHelper.dataType.BossChosen:
			// 	data.allocate(3);
			// 	data.putChar(1, );
			// 	break;
			default:
				data = ByteBuffer.allocate(1);
				break;
		}

		data.putInt(0, type.ordinal());

		return data;
	}


	public static void createLobby() {
		matcher.createLobby(SteamMatchmaking.LobbyType.Private, 8);
	}


	// Things to do here
	//   take care of joins/parts/messages from lobby
	//   gracefully handle connections and disconnections
	//   send messages via the system


}