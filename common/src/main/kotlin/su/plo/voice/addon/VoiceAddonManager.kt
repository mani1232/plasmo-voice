package su.plo.voice.addon

import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.inject.Guice
import org.apache.logging.log4j.LogManager
import su.plo.voice.BaseVoice
import su.plo.voice.api.addon.*
import su.plo.voice.api.addon.annotation.Addon
import java.util.*

class VoiceAddonManager(
    private val voice: BaseVoice
) : AddonManager {

    private val addonByInstance: MutableMap<Any, AddonContainer> = Maps.newHashMap()
    private val addonById: MutableMap<String, AddonContainer> = Maps.newHashMap()

    private var initialized = false

    init {
        // register PlasmoVoice as an addon
        val voiceAddon: AddonContainer = PlasmoVoiceAddon(voice, AddonLoaderScope.ANY)
        addonById["plasmovoice"] = voiceAddon
        addonByInstance[voice] = voiceAddon
    }

    @Synchronized
    override fun load(addonObject: Any) {
        val addonClass: Class<*> = addonObject.javaClass
        require(addonClass.isAnnotationPresent(Addon::class.java)) { "Addon object must be annotated with @Addon" }

        val addon = addonClass.getAnnotation(
            Addon::class.java
        )

        val addonContainer = VoiceAddon(
            addon.id,
            if (Strings.emptyToNull(addon.name) == null) addon.id else addon.name,
            addon.scope,
            addon.version,
            Lists.newArrayList(*addon.authors),
            addon.dependencies.map { dependency ->
                AddonDependency(dependency.id, dependency.optional, dependency.mod)
            },
            addonClass
        )

        addonContainer.dependencies.filter { !it.isOptional && !it.isMod }.forEach { dependency ->
            if (!addonById.containsKey(dependency.id)) {
                LOGGER.error("Addon \"{}\" is missing dependency \"{}\"", addonContainer.id, dependency.id)
                return
            }
        }

        addonContainer.setInstance(addonObject)
        loadAddon(addonContainer)
    }

    override fun isLoaded(id: String): Boolean {
        return addonById.containsKey(id)
    }

    override fun getAddon(id: String): Optional<AddonContainer> {
        return Optional.ofNullable(addonById[id])
    }

    override fun getAddon(instance: Any): Optional<AddonContainer> {
        return Optional.ofNullable(addonByInstance[instance])
    }

    @Synchronized
    fun initializeLoadedAddons() {
        if (initialized) return

        addonById.values
            .filter { it.id != "plasmovoice" }
            .forEach(this::initializeAddon)

        this.initialized = true
    }

    @Synchronized
    fun clear() {
        addonById.values.forEach { addon ->
            if (addon.id == "plasmovoice") return@forEach

            voice.eventBus.unregister(addon.instance.get())
            LOGGER.info(
                "Addon {} v{} by {} unloaded",
                addon.id,
                addon.version,
                java.lang.String.join(", ", addon.authors)
            )
        }

        this.initialized = false
    }

    private fun loadAddon(addon: AddonContainer) {
        if (initialized) {
            // unregister old event listeners
            addonById[addon.id]?.let { oldAddon ->
                voice.eventBus.unregister(oldAddon.instance.get())
            }
        }

        val addonInstance = addon.instance.get()

        // inject guice module
        val injector = Guice.createInjector(voice.createInjectModule())
        injector.injectMembers(addonInstance)

        addonById[addon.id] = addon
        addonByInstance[addonInstance] = addon

        if (initialized) initializeAddon(addon)
    }

    private fun initializeAddon(addon: AddonContainer) {
        val addonInstance = addon.instance.get()

        if (addonInstance is AddonInitializer) {
            addonInstance.onAddonInitialize()
        }

        voice.eventBus.register(addonInstance, addonInstance)

        LOGGER.info(
            "{} v{} by {} loaded",
            addon.id,
            addon.version,
            java.lang.String.join(", ", addon.authors)
        )
    }

    companion object {

        private val LOGGER = LogManager.getLogger(VoiceAddonManager::class.java)
    }
}
