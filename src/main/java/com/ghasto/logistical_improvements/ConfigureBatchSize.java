package com.ghasto.logistical_improvements;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record ConfigureBatchSize(FactoryPanelPosition position, int value) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LogisticalImprovements.MODID, "configure_batch_size");
    public static final Type<ConfigureBatchSize> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, ConfigureBatchSize> STREAM_CODEC = StreamCodec.composite(
            FactoryPanelPosition.STREAM_CODEC,
            ConfigureBatchSize::position,
            ByteBufCodecs.INT,
            ConfigureBatchSize::value,
            ConfigureBatchSize::new
    );

    @Override
    public @NotNull Type<ConfigureBatchSize> type() {
        return TYPE;
    }
}
