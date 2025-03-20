package bread_experts_group.util

import kotlin.collections.forEachIndexed
import kotlin.reflect.full.memberProperties
import kotlin.text.replace

object ToStringUtil {
	fun toString(obj: Any): String {
		var initial = "${obj::class.simpleName}(\n"
		val properties = obj::class.memberProperties
		properties.forEachIndexed { i, property ->
			val value = property.call(obj)
			val valueStr =
				if (value != null)
					(if (value is SmartToString) value.toString() else "${value::class.simpleName}($value)")
						.replace("\n", "\n\t")
				else "null"
			initial += "\t${property.name}=$valueStr${if (i == (properties.size - 1)) "" else ",\n"}"
		}
		return "$initial\n)"
	}

	open class SmartToString {
		final override fun toString(): String = this@ToStringUtil.toString(this)
	}
}