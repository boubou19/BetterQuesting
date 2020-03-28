package betterquesting.network.handlers;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumPacketAction;
import betterquesting.api.network.IPacketHandler;
import betterquesting.api.questing.IQuestLine;
import betterquesting.core.BetterQuesting;
import betterquesting.network.PacketSender;
import betterquesting.network.PacketTypeNative;
import betterquesting.questing.QuestLine;
import betterquesting.questing.QuestLineDatabase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Level;

public class PktHandlerLineEdit implements IPacketHandler
{
	@Override
	public ResourceLocation getRegistryName()
	{
		return PacketTypeNative.LINE_EDIT.GetLocation();
	}
	
	@Override
	public void handleServer(NBTTagCompound data, EntityPlayerMP sender)
	{
		if(sender == null || sender.mcServer == null)
		{
			return;
		}
		
		boolean isOP = sender.mcServer.getConfigurationManager().func_152596_g(sender.getGameProfile());
		
		if(!isOP)
		{
			BetterQuesting.logger.log(Level.WARN, "Player " + sender.getCommandSenderName() + " (UUID:" + QuestingAPI.getQuestingUUID(sender) + ") tried to edit quest lines without OP permissions!");
			sender.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.RED + "You need to be OP to edit quests!"));
			return; // Player is not operator. Do nothing
		}
		
		int aID = !data.hasKey("action")? -1 : data.getInteger("action");
		int lID = !data.hasKey("lineID")? -1 : data.getInteger("lineID");
		int idx = !data.hasKey("order")? -1 : data.getInteger("order");
		IQuestLine questLine = QuestLineDatabase.INSTANCE.getValue(lID);
		
		if(aID < 0 || aID >= EnumPacketAction.values().length)
		{
			return;
		}
		
		EnumPacketAction action = EnumPacketAction.values()[aID];
		
		if(action == EnumPacketAction.ADD) 
		{
			IQuestLine nq = new QuestLine();
			int nID = QuestLineDatabase.INSTANCE.nextID();
			
			if(data.hasKey("data") && lID >= 0)
			{
				nID = lID;
				
				NBTTagCompound base = data.getCompoundTag("data");
				nq.readFromNBT(base.getCompoundTag("line"), false);
			}
			
			QuestLineDatabase.INSTANCE.add(nID, nq);
			PacketSender.INSTANCE.sendToAll(nq.getSyncPacket());
		} else if(action == EnumPacketAction.EDIT && questLine != null) // Edit quest lines
		{
			questLine.readPacket(data);
			
			if(idx >= 0 && QuestLineDatabase.INSTANCE.getOrderIndex(lID) != idx)
			{
				QuestLineDatabase.INSTANCE.setOrderIndex(lID, idx);
				PacketSender.INSTANCE.sendToAll(QuestLineDatabase.INSTANCE.getSyncPacket());
			} else
			{
				PacketSender.INSTANCE.sendToAll(questLine.getSyncPacket());
			}
		} else if(action == EnumPacketAction.REMOVE && questLine != null)
		{
			QuestLineDatabase.INSTANCE.removeID(lID);
			PacketSender.INSTANCE.sendToAll(QuestLineDatabase.INSTANCE.getSyncPacket());
		}
	}
	
	@Override
	public void handleClient(NBTTagCompound data)
	{
	}
}