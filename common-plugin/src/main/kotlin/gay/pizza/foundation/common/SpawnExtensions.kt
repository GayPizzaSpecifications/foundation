package gay.pizza.foundation.common

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import kotlin.reflect.KClass

fun <T: Entity> World.spawn(location: Location, clazz: KClass<T>): T = spawn(location, clazz.java)

fun <T: Entity> Player.spawn(clazz: KClass<T>): T = spawn(clazz.java)
fun <T: Entity> Player.spawn(clazz: Class<T>): T = world.spawn(location, clazz)
