package com.ghasto.logistical_improvements.mixin;

import com.ghasto.logistical_improvements.BatchSizeAccessor;
import com.ghasto.logistical_improvements.ConfigureBatchSize;
import com.ghasto.logistical_improvements.LogisticalImprovements;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FactoryPanelScreen.class, remap = false)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {
    @Shadow
    private FactoryPanelBehaviour behaviour;
    @Shadow private boolean restocker;
    @Shadow private boolean sendReset;
    @Unique
    private ScrollInput minimumBatchSize;

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if(!restocker) return;
        minimumBatchSize = new ScrollInput(guiLeft + 8, guiTop + windowHeight -24, 31, 16)
                .withRange(1, 577)
                .withStepFunction(c -> (c.currentValue < 64 && c.forward) ? c.shift ? 4 : 1 : 64)
                .titled(Component.translatable("tooltip.vanilla_logistics.minimum_batch_size"));
        minimumBatchSize.setState(((BatchSizeAccessor)behaviour).getMinimumBatchSize());
        addRenderableWidget(minimumBatchSize);
    }

    @Inject(method = "renderWindow", at = @At("TAIL"))
    private void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if(minimumBatchSize == null) return;
        if(!restocker) return;

        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(LogisticalImprovements.MODID, "textures/gui/batch_size_prompt.png"),
                minimumBatchSize.getX() - 1,
                minimumBatchSize.getY() - 1,
                0,
                0,
                42,
                18,
                64,
                64
        );

        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(LogisticalImprovements.MODID, "textures/gui/bar.png"),
                guiLeft + 51,
                guiTop + windowHeight - 32,
                0,
                0,
                2,
                32,
                32,
                32
        );

        int state = minimumBatchSize.getState();
        graphics.drawCenteredString(font, Component.literal(state == 1 ? "/" : state + ""), minimumBatchSize.getX() + 15 , minimumBatchSize.getY() + 4, 0xffeeeeee);
    }

    @Inject(method = "sendIt", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V", shift = At.Shift.AFTER))
    private void sendIt(FactoryPanelPosition toRemove, boolean clearPromises, CallbackInfo ci) {
        if(!restocker) return;
        CatnipServices.NETWORK.sendToServer(new ConfigureBatchSize(behaviour.getPanelPosition(), sendReset ? 1 : minimumBatchSize.getState()));
    }
}
