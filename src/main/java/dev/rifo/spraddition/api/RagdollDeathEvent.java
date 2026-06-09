package dev.rifo.spraddition.api;

import dev.leo.sableplayerragdoll.api.PlayerlessRagdollSession;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public class RagdollDeathEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private PlayerlessRagdollSession session;

    public RagdollDeathEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public ServerPlayer getEntity() {
        return this.player;
    }

    @Nullable
    public PlayerlessRagdollSession session() {
        return this.session;
    }

    @Nullable
    public PlayerlessRagdollSession getSession() {
        return this.session;
    }

    public void setSession(PlayerlessRagdollSession session) {
        this.session = session;
    }
}

