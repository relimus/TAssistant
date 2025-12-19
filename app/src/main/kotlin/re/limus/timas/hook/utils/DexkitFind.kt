package re.limus.timas.hook.utils

import top.sacz.xphelper.dexkit.DexFinder
import top.sacz.xphelper.ext.toClass

object DexkitFind {
    private val funcItemInfo = "com.tencent.tim.function.FunctionItemInfo".toClass()
    private val mineA = "com.tencent.mobileqq.activity.tim.mine.a".toClass()
    private val mineB = "com.tencent.mobileqq.activity.tim.mine.b".toClass()

    val addMineItem = DexFinder.findMethod {
        declaredClass = funcItemInfo
        paramCount = 8
    }.firstConstructor()!!

    val mineA2u = DexFinder.findMethod {
        declaredClass = mineA
        methodName = "u"
        returnType = Void.TYPE
        paramCount = 1
        parameters = arrayOf(List::class.java)
    }.first()!!

    val mineB2i = DexFinder.findMethod {
        declaredClass = mineB
        returnType = Void.TYPE
        methodName = "i"
        parameters = arrayOf(funcItemInfo)
    }.first()!!

    val mineB2h = DexFinder.findMethod {
        declaredClass = mineB
        returnType = Void.TYPE
        paramCount = 1
        parameters = arrayOf(funcItemInfo)
        usedString = arrayOf("itemInfo")
    }.first()!!
}