package re.limus.timas.api

import android.content.Context
import android.view.View
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import re.limus.timas.annotations.ApiItems
import re.limus.timas.hook.base.XBridge
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.reflect.ClassUtils
import top.sacz.xphelper.reflect.Ignore
import top.sacz.xphelper.reflect.MethodUtils
import java.util.concurrent.Callable

@ApiItems
object TIMCustomMenu : XBridge() {

    override fun onHook(ctx: Context, loader: ClassLoader) {
        try {
            val findMethod =
                MethodUtils.create("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout")
                    .returnType(View::class.java)
                    .params(
                        Int::class.javaPrimitiveType,
                        Ignore::class.java,
                        Boolean::class.javaPrimitiveType,
                        FloatArray::class.java
                    )
                    .first()

            if (findMethod == null) {
                return
            }

            val parameters = findMethod.parameters
            if (parameters.size <= 1) {
                return
            }

            if (parameters[1] == null) {
                return
            }

            baseMenuItemClass = parameters[1]!!.getType()
        } catch (_: Exception) {
            baseMenuItemClass = null
        }
    }

    /**
     * 抽象类
     */
    private var baseMenuItemClass: Class<*>? = null

    fun createMenuItem(
        aioMsgItem: Any?,
        text: Any,
        id: Int,
        icon: Int,
        callable: Callable<*>
    ): Any? {
        val generatedDir = XpHelper.context.getDir("generated", Context.MODE_PRIVATE)
        try {
            if (baseMenuItemClass == null) {
                return null
            }

            val make: DynamicType.Unloaded<*> = ByteBuddy().subclass(baseMenuItemClass) //标题
                .method(ElementMatchers.named("f"))
                .intercept(FixedValue.value(text)) //新方法 不知道是啥
                .method(ElementMatchers.named("e")).intercept(FixedValue.value(text)) //图标
                .method(ElementMatchers.named("b")).intercept(FixedValue.value(icon)) //点击回调
                .method(ElementMatchers.returns(Void.TYPE))
                .intercept(MethodCall.call(callable)) //id
                .method(ElementMatchers.named("c")).intercept(FixedValue.value(id))
                .make()

            val generatedClass: Class<*> =
                make.load(
                    baseMenuItemClass!!.getClassLoader(),
                    AndroidClassLoadingStrategy.Wrapping(generatedDir)
                )
                    .getLoaded()
            return generatedClass.getDeclaredConstructor(ClassUtils.findClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"))
                .newInstance(aioMsgItem)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}