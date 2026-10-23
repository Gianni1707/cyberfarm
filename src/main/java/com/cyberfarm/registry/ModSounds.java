package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CyberFarm's own sound events (files in sounds.json, reusing vanilla audio). Every machine and drone
 * sound goes through here, so the Muffler can recognise them by the cyberfarm namespace.
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CyberFarm.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_FLY = register("drone.fly");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_TAKEOFF = register("drone.takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_LAND = register("drone.land");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_UNFOLD = register("drone.unfold");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXTRACTOR_WORK = register("methane_extractor.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXTRACTOR_ACTIVATE = register("methane_extractor.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXTRACTOR_DEACTIVATE = register("methane_extractor.deactivate");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_ACTIVATE = register("reactor.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_DEACTIVATE = register("reactor.deactivate");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_HUM = register("reactor.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_ALARM = register("reactor.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOLANT_MIXER_WORK = register("coolant_mixer.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_TURBINE_WORK = register("steam_turbine.work");
    public static final DeferredHolder<SoundEvent, SoundEvent> REPROCESSOR_WORK = register("waste_reprocessor.work");
    /** Not a machine: the Muffler leaves it alone (see CyberFarmClient). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = register("geiger.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> GENERATOR_CRACKLE = register("combustion_generator.crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_WORK = register("drone_assembler.work");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CyberFarm.id(name)));
    }
}
