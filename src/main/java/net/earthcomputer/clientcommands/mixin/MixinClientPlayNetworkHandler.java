package net.earthcomputer.clientcommands.mixin;

import net.cortex.clientAddon.cracker.SeedCracker;
import net.earthcomputer.clientcommands.ServerBrandManager;
import net.earthcomputer.clientcommands.TempRules;
import net.earthcomputer.clientcommands.features.FishingCracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    public void onOnEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        SeedCracker.onEntityCreation(packet);

        if (FishingCracker.canManipulateFishing()) {
            if (packet.getEntityData() == player.getId() && packet.getEntityTypeId() == EntityType.FISHING_BOBBER) {
                FishingCracker.processBobberSpawn(packet.getUuid(), new Vec3d(packet.getX(), packet.getY(), packet.getZ()), new Vec3d(packet.getVelocityX(), packet.getVelocityY(), packet.getVelocityZ()));
            }
        }
    }

    @Inject(method = "onEntitySpawn", at = @At("HEAD"))
    public void onOnEntitySpawnPre(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        // Called on network thread first, FishingCracker.waitingForFishingRod

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        if (!FishingCracker.canManipulateFishing() || packet.getEntityData() != player.getId() || packet.getEntityTypeId() != EntityType.FISHING_BOBBER) {
            return;
        }

        FishingCracker.onFishingBobberEntity();
    }

    @Inject(method = "onExperienceOrbSpawn", at = @At("TAIL"))
    public void onOnExperienceOrbSpawn(ExperienceOrbSpawnS2CPacket packet, CallbackInfo ci) {
        if (FishingCracker.canManipulateFishing()) {
            FishingCracker.processExperienceOrbSpawn(packet.getX(), packet.getY(), packet.getZ(), packet.getExperience());
        }
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void onOnWorldTimeUpdatePre(CallbackInfo ci) {
        if (TempRules.getFishingManipulation().isEnabled() && !MinecraftClient.getInstance().isOnThread()) {
            FishingCracker.onTimeSync();
        }
    }

    @Inject(method = "onCustomPayload", at = @At("TAIL"))
    public void onOnCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if (CustomPayloadS2CPacket.BRAND.equals(packet.getChannel())) {
            ServerBrandManager.setServerBrand(this.client.player.getServerBrand());
        }
    }

}
