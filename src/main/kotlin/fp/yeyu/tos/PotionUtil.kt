package fp.yeyu.tos

import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.potion.Potion
import net.minecraft.potion.Potions
import net.minecraft.recipe.BrewingRecipeRegistry
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object PotionUtil {
    private val quickRegeneration = StatusEffectInstance(StatusEffects.REGENERATION, 32, 50)
    private val healthBoostIPotion = Potion("tos_health_boost_potion", StatusEffectInstance(StatusEffects.HEALTH_BOOST, 3600, 1), quickRegeneration)
    private val healthBoostIIPotion = Potion("tos_health_boost_ii_potion", StatusEffectInstance(StatusEffects.HEALTH_BOOST, 4800, 3), quickRegeneration)
    private val healthBoostIPotionRecipe = BrewingRecipeRegistry.Recipe(Potions.MUNDANE, Ingredient.ofItems(TotemOfShadowEntry.spiritEssence), healthBoostIPotion)
    private val healthBoostIIPotionRecipe = BrewingRecipeRegistry.Recipe(Potions.MUNDANE, Ingredient.ofItems(TotemOfShadowEntry.spiritEssenceTrio), healthBoostIIPotion)
    private val healthBoostI_Id = Identifier(TotemOfShadowEntry.NAMESPACE, "tos_health_boost_potion")
    private val healthBoostII_Id = Identifier(TotemOfShadowEntry.NAMESPACE, "tos_health_boost_ii_potion")

    fun addHealthBoostPotionAndRecipe() {
        Registry.register(Registry.POTION, healthBoostI_Id, healthBoostIPotion)
        Registry.register(Registry.POTION, healthBoostII_Id, healthBoostIIPotion)
        BrewingRecipeRegistry.POTION_RECIPES.add(healthBoostIPotionRecipe)
        BrewingRecipeRegistry.POTION_RECIPES.add(healthBoostIIPotionRecipe)
    }
}