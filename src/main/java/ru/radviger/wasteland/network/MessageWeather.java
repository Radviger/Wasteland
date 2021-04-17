package com.legacy.wasteland.network;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageWeather implements IMessage {
    private WeatherType oldType, newType;
    private float windX, windZ;

    public MessageWeather() {}

    public MessageWeather(WeatherType oldType, WeatherType newType, float windX, float windZ) {
        this.oldType = oldType;
        this.newType = newType;
        this.windX = windX;
        this.windZ = windZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.oldType = WeatherType.values()[buf.readByte()];
        this.newType = WeatherType.values()[buf.readByte()];
        this.windX = buf.readFloat();
        this.windZ = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.oldType.ordinal());
        buf.writeByte(this.newType.ordinal());
        buf.writeFloat(this.windX);
        buf.writeFloat(this.windZ);
    }

    public static class Handler implements IMessageHandler<MessageWeather, IMessage> {
        @Override
        public IMessage onMessage(MessageWeather message, MessageContext context) {
            if (context.side == Side.CLIENT) {
                handleClient(message);
            }
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(MessageWeather message) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                WorldClient world = mc.world;
                if (world != null) {
                    WastelandWorldData data = Wasteland.PROXY.getData(world);
                    if (data == null) {
                        data = new WastelandWorldData();
                        Wasteland.PROXY.data.put(world.provider.getDimensionType(), data);
                    }
                    data.setWeatherType(message.oldType, message.newType);
                    data.setWind(message.windX, message.windZ);
                }
            });
        }
    }
}
