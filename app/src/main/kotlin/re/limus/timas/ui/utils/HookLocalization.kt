package re.limus.timas.ui.utils

import android.content.Context
import re.limus.timas.R
import re.limus.timas.hook.base.SwitchHook
import re.limus.timas.hook.generated.HookRegistry

/** Resolves hook names and descriptions from the active Android locale. */
object HookLocalization {

    private data class Text(val name: Int, val description: Int? = null)

    private val texts = mapOf(
        "AddAEmail2Mine" to Text(R.string.hook_add_email_name, R.string.hook_add_email_description),
        "AddChannel2Mine" to Text(R.string.hook_add_channel_name, R.string.hook_add_channel_description),
        "AllLeftSlip" to Text(R.string.hook_all_left_slip_name, R.string.hook_all_left_slip_description),
        "AutoOriginalPics" to Text(R.string.hook_auto_original_pics_name, R.string.hook_auto_original_pics_description),
        "BlockLinkInfoCard" to Text(R.string.hook_block_link_info_card_name),
        "CheckBannedUserCard" to Text(R.string.hook_check_banned_user_card_name, R.string.hook_check_banned_user_card_description),
        "CircleAvatar" to Text(R.string.hook_circle_avatar_name, R.string.hook_circle_avatar_description),
        "CustomDownloadDirectory" to Text(R.string.hook_custom_download_directory_name, R.string.hook_custom_download_directory_description),
        "DisableAutoMention" to Text(R.string.hook_disable_auto_mention_name),
        "Emoji2Sticker" to Text(R.string.hook_emoji_to_sticker_name, R.string.hook_emoji_to_sticker_description),
        "ForceMemberLevel" to Text(R.string.hook_force_member_level_name, R.string.hook_force_member_level_description),
        "ForcePadMode" to Text(R.string.hook_force_pad_mode_name, R.string.hook_force_pad_mode_description),
        "GalleryBgTp" to Text(R.string.hook_gallery_background_name, R.string.hook_gallery_background_description),
        "HideQzoneAD" to Text(R.string.hook_hide_qzone_ad_name, R.string.hook_hide_qzone_ad_description),
        "HideQzoneVipTip" to Text(R.string.hook_hide_qzone_vip_name, R.string.hook_hide_qzone_vip_description),
        "MenuMessageRepeat" to Text(R.string.hook_menu_message_repeat_name, R.string.hook_menu_message_repeat_description),
        "PreventRevokeMsg" to Text(R.string.hook_prevent_revoke_name, R.string.hook_prevent_revoke_description),
        "PttForward" to Text(R.string.hook_ptt_forward_name, R.string.hook_ptt_forward_description),
        "RemoveForwardNumbersLimit" to Text(R.string.hook_remove_forward_limit_name, R.string.hook_remove_forward_limit_description),
        "RemoveQrScanAuth" to Text(R.string.hook_remove_qr_scan_auth_name),
        "RenameApk" to Text(R.string.hook_rename_apk_name, R.string.hook_rename_apk_description),
        "SAFSelector" to Text(R.string.hook_saf_selector_name, R.string.hook_saf_selector_description),
        "SendFavoritePtt" to Text(R.string.hook_send_favorite_ptt_name, R.string.hook_send_favorite_ptt_description),
        "SharedCardClickable" to Text(R.string.hook_shared_card_clickable_name, R.string.hook_shared_card_clickable_description),
        "ShowAccurateGaggedTime" to Text(R.string.hook_show_accurate_gagged_time_name, R.string.hook_show_accurate_gagged_time_description),
        "ShowDownloadTimes" to Text(R.string.hook_show_download_times_name, R.string.hook_show_download_times_description),
        "ShowMsgDetailCount" to Text(R.string.hook_show_msg_detail_count_name, R.string.hook_show_msg_detail_count_description),
        "SortAtList" to Text(R.string.hook_sort_at_list_name, R.string.hook_sort_at_list_description),
        "SystemEmojiStyle" to Text(R.string.hook_system_emoji_style_name, R.string.hook_system_emoji_style_description),
        "TroopFilePermanent" to Text(R.string.hook_troop_file_permanent_name, R.string.hook_troop_file_permanent_description),
        "TroopSettingEssence" to Text(R.string.hook_troop_setting_essence_name, R.string.hook_troop_setting_essence_description)
    )

    fun name(context: Context, hook: SwitchHook): String {
        val text = texts[HookRegistry.hookClassNames[hook] ?: hook.javaClass.simpleName] ?: return hook.name
        return context.getString(text.name)
    }

    fun description(context: Context, hook: SwitchHook): CharSequence? {
        val text = texts[HookRegistry.hookClassNames[hook] ?: hook.javaClass.simpleName]
            ?: return hook.description
        return text.description?.let(context::getString) ?: hook.description
    }
}
