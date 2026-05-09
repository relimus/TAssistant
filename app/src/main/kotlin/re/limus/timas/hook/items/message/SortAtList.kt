package re.limus.timas.hook.items.message

import android.content.Context
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook

@RegisterToUI
object SortAtList : SwitchHook() {

    override val name = "@列表重新排序"

    override val description = "优先排序群主以及管理"

    override val category = UiCategory.MESSAGE

    private var memberInfoClass: Class<*>? = null

    override fun onHook(ctx: Context, loader: ClassLoader) {
        memberInfoClass = loader.loadClass("com.tencent.qqnt.kernel.nativeinterface.MemberInfo")

        val submitListEventClass = loader.loadClass("com.tencent.mobileqq.aio.input.at.common.SubmitListEvent")
        val getItemListMethod = submitListEventClass.getDeclaredMethod("getItemList")

        getItemListMethod.hookAfter {
            val originalList = result as? List<*> ?: return@hookAfter

            val sortedList = originalList.sortedBy { item ->
                rank(item)
            }

            result = sortedList
        }
    }

    private fun rank(item: Any?): Int {
        if (item == null) return 99
        val targetClass = memberInfoClass ?: return 0

        try {
            val memberInfoField = item.javaClass.declaredFields.find { field ->
                field.type == targetClass
            } ?: return 0

            memberInfoField.isAccessible = true
            val memberInfo = memberInfoField.get(item) ?: return 0

            val isRobotField = targetClass.getDeclaredField("isRobot")
            isRobotField.isAccessible = true
            val isRobot = isRobotField.getBoolean(memberInfo)

            if (isRobot) return 3

            val roleField = targetClass.getDeclaredField("role")
            roleField.isAccessible = true
            val roleObj = roleField.get(memberInfo)
            val roleName = roleObj?.toString() ?: ""

            return when {
                roleName.contains("OWNER") -> 1
                roleName.contains("ADMIN") -> 2
                roleName.contains("MEMBER") -> 4
                else -> 5
            }
        } catch (e: Exception) {
            return 0
        }
    }
}
