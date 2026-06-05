package iso2t.hi.core.registries;

import com.mojang.serialization.Codec;
import iso2t.hi.core.HeavyInventories;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class DataAttachments {

	public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HeavyInventories.MODID);

	public static final Supplier<AttachmentType<Float>> CURRENT_WEIGHT = register("current_weight", () -> AttachmentType.builder(() -> 0f).serialize(Codec.FLOAT.fieldOf("current_weight"))
			.sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT).copyOnDeath().build());
	public static final Supplier<AttachmentType<Float>> MAX_WEIGHT = register("max_weight", () -> AttachmentType.builder(() -> 1_250f).serialize(Codec.FLOAT.fieldOf("max_weight"))
			.sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT).copyOnDeath().build());

	// 0: not encumbered
	// 1: encumbered
	// 2: overencumbered
	public static final Supplier<AttachmentType<Integer>> ENCUMBRANCE_LEVEL = register("encumbrance_level", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("encumbrance_level"))
			.sync((holder, to) -> holder == to, ByteBufCodecs.INT).copyOnDeath().build());

	static <T> Supplier<AttachmentType<T>> register (String name, Supplier<AttachmentType<T>> supplier) {
		return REGISTRY.register(name, supplier);
	}

}
