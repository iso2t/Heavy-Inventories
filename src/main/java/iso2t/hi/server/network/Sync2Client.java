package iso2t.hi.server.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Sync2Client {

	public static class Weight implements AttachmentSyncHandler<Float> {
		@Override
		public void write (@NonNull RegistryFriendlyByteBuf buf, @NonNull Float attachment, boolean initialSync) {
			if (initialSync) {

			}
		}

		@Override
		public @Nullable Float read (@NonNull IAttachmentHolder holder, @NonNull RegistryFriendlyByteBuf buf, @Nullable Float previousValue) {
			return 0f;
		}

		@Override
		public boolean sendToPlayer (@NonNull IAttachmentHolder holder, @NonNull ServerPlayer to) {
			return AttachmentSyncHandler.super.sendToPlayer(holder, to);
		}
	}

}
