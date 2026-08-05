package re.limus.timas.hook.items.style

import android.content.Context
import re.limus.timas.R
import re.limus.timas.annotations.RegisterToUI
import re.limus.timas.annotations.UiCategory
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.utils.AvatarFinder

@RegisterToUI
object CircleAvatar : SwitchHook(
    R.string.hook_circle_avatar_name,
    R.string.hook_circle_avatar_description
) {

    override val category = UiCategory.STYLE

    override val needRestart = true

    override fun onHook(ctx: Context, loader: ClassLoader) {
        AvatarFinder().hookAvatar()
    }
}
