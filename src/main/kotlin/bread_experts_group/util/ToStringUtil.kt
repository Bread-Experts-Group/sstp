package bread_experts_group.util

import kotlin.reflect.full.memberProperties

object ToStringUtil {
	fun toString(obj: Any): String =
		if (verbosity < 0) {
			var initial = "${obj::class.simpleName}(\n"
			val properties = obj::class.memberProperties
			properties.forEachIndexed { i, property ->
				val value = property.call(obj)
				val valueStr = value?.let {
					if (value is SmartToString) value.toString()
					else "${value::class.simpleName}($value)"
				}?.replace("\n", "\n\t") ?: "null"
				initial += "\t${property.name}=$valueStr${if (i == (properties.size - 1)) "" else ",\n"}"
			}
			"$initial\n)"
		} else if (verbosity > 0) {
			if (obj is SmartToString) {
				obj.gist()
					.split('\n', ignoreCase = true)
					.let { if (it.size > verbosity) it.slice(0 until verbosity) else it }
					.joinToString("\n")
			} else obj.toString()
		} else ""

	abstract class SmartToString {
		abstract fun gist(): String
		final override fun toString(): String = this@ToStringUtil.toString(this)
	}
}