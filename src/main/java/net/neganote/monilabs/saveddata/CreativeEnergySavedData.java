package net.neganote.monilabs.saveddata;

import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class CreativeEnergySavedData extends SavedData {

    public static String DATA_NAME = "creativeEnergyData";
    public static String ENERGY_OWNERS = "creativeEnergyOwners";

    private final Map<UUID, Boolean> ownersMap = new HashMap<>();

    private CreativeEnergySavedData() {}

    public CreativeEnergySavedData(CompoundTag tag) {
        var ownerList = tag.getList(ENERGY_OWNERS, CompoundTag.TAG_COMPOUND);
        for (Tag t : ownerList) {
            CompoundTag ownerTag = (CompoundTag) t;
            long ownerUUIDMSB = ownerTag.getLong("ownerUUIDMSB");
            long ownerUUIDLSB = ownerTag.getLong("ownerUUIDLSB");
            boolean enabled = ownerTag.getBoolean("enabled");
            UUID ownerUUID = new UUID(ownerUUIDMSB, ownerUUIDLSB);
            ownersMap.put(ownerUUID, enabled);
        }
    }

    public static CreativeEnergySavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel
            .getDataStorage()
            .computeIfAbsent(
                CreativeEnergySavedData::new,
                CreativeEnergySavedData::new,
                DATA_NAME
            );
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        ListTag ownerList = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : ownersMap.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            var ownerUUID = entry.getKey();
            ownerTag.putLong(
                "ownerUUIDMSB",
                ownerUUID.getMostSignificantBits()
            );
            ownerTag.putLong(
                "ownerUUIDLSB",
                ownerUUID.getLeastSignificantBits()
            );
            ownerTag.putBoolean("enabled", entry.getValue());
            ownerList.add(ownerTag);
        }
        nbt.put(ENERGY_OWNERS, ownerList);
        return nbt;
    }

    public boolean isEnabledFor(UUID uuid) {
        MachineOwner owner = MachineOwner.getOwner(uuid);
        if (owner == null) {
            return false;
        }
        for (Map.Entry<UUID, Boolean> entry : ownersMap.entrySet()) {
            if (entry.getValue() && owner.isPlayerInTeam(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        boolean isEnabled = isEnabledFor(uuid);
        if (isEnabled != enabled) {
            ownersMap.put(uuid, enabled);
            setDirty();
        }
    }
}
