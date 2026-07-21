package re.limus.timas.hook.items.file.helper

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.get

internal object PbandkMessageAccessor {

    private data class MessageAccess(
        val getValueMap: Method,
        val putValue: Method
    )

    private val messageAccessCache = ConcurrentHashMap<Class<*>, MessageAccess>()
    private val descriptorReadCache = ConcurrentHashMap<Class<*>, Method>()

    fun isMessage(type: Class<*>): Boolean {
        return runCatching { getMessageAccess(type) }.isSuccess
    }

    fun getValue(message: Any, vararg tags: Int): Any? {
        var current: Any? = message
        for (tag in tags) {
            current = current?.let { getDirectValue(it, tag) }
        }
        return current
    }

    fun replaceValue(message: Any, vararg tags: Int, transform: (Any?) -> Any?) {
        require(tags.isNotEmpty()) { "Tag path must not be empty" }

        val oldValue = getValue(message, *tags)
        val parent = if (tags.size == 1) {
            message
        } else {
            getValue(message, *tags.copyOf(tags.size - 1)) ?: return
        }
        val newValue = transform(oldValue)
        getMessageAccess(parent.javaClass).putValue.invoke(parent, tags.last(), newValue)
    }

    private fun getDirectValue(message: Any, tag: Int): Any? {
        val valueMap = getMessageAccess(message.javaClass).getValueMap.invoke(message) as? Map<*, *>
            ?: return null
        val descriptor = valueMap[tag] ?: return null
        return getDescriptorRead(descriptor.javaClass).invoke(descriptor)
    }

    private fun getMessageAccess(type: Class<*>): MessageAccess {
        messageAccessCache[type]?.let { return it }

        val access = classHierarchy(type).firstNotNullOfOrNull { current ->
            val methods = current.declaredMethods.filterNot { Modifier.isStatic(it.modifiers) }
            val getValueMap = methods.firstOrNull {
                it.parameterCount == 0 && Map::class.java.isAssignableFrom(it.returnType)
            }
            val putValue = methods.firstOrNull {
                it.returnType == Void.TYPE &&
                    it.parameterTypes.contentEquals(
                        arrayOf(Int::class.javaPrimitiveType, Any::class.java)
                    )
            }

            if (getValueMap == null || putValue == null) {
                null
            } else {
                getValueMap.isAccessible = true
                putValue.isAccessible = true
                MessageAccess(getValueMap, putValue)
            }
        } ?: error("PBandK message methods not found: ${type.name}")

        return messageAccessCache.putIfAbsent(type, access) ?: access
    }

    private fun getDescriptorRead(type: Class<*>): Method {
        descriptorReadCache[type]?.let { return it }

        val readValue = classHierarchy(type).firstNotNullOfOrNull { current ->
            current.declaredMethods.firstOrNull {
                !Modifier.isStatic(it.modifiers) &&
                    it.parameterCount == 0 &&
                    it.returnType == Any::class.java &&
                    !it.isSynthetic
            } ?: current.declaredMethods.firstOrNull {
                !Modifier.isStatic(it.modifiers) &&
                    it.parameterCount == 0 &&
                    it.returnType == Any::class.java
            }
        } ?: error("PBandK field descriptor reader not found: ${type.name}")

        readValue.isAccessible = true
        return descriptorReadCache.putIfAbsent(type, readValue) ?: readValue
    }

    private fun classHierarchy(type: Class<*>): Sequence<Class<*>> = sequence {
        val queue = ArrayDeque<Class<*>>()
        val visited = mutableSetOf<Class<*>>()
        queue.add(type)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == Any::class.java || !visited.add(current)) continue

            yield(current)
            current.superclass?.let(queue::addLast)
            current.interfaces.forEach(queue::addLast)
        }
    }
}