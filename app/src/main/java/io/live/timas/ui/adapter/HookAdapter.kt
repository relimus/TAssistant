package io.live.timas.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import io.live.timas.R
import io.live.timas.databinding.PreferenceBasicDownBinding
import io.live.timas.databinding.PreferenceBasicMidBinding
import io.live.timas.databinding.PreferenceBasicSingleBinding
import io.live.timas.databinding.PreferenceBasicTopBinding
import io.live.timas.hook.HookManager
import io.live.timas.ui.model.HookItem

/**
 * Hook 列表适配器
 * 支持 4 种视图类型：SINGLE, TOP, MID, DOWN
 */
class HookAdapter(
    private val hooks: List<HookItem>
) : RecyclerView.Adapter<HookAdapter.BaseViewHolder>() {

    /**
     * 视图类型枚举
     */
    private enum class ViewType(val value: Int) {
        SINGLE(0),
        TOP(1),
        MID(2),
        DOWN(3)
    }

    override fun getItemViewType(position: Int): Int {
        val totalCount = itemCount
        return when {
            totalCount == 1 -> ViewType.SINGLE.value
            position == 0 -> ViewType.TOP.value
            position == totalCount - 1 -> ViewType.DOWN.value
            else -> ViewType.MID.value
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            ViewType.SINGLE.value -> {
                val binding = PreferenceBasicSingleBinding.inflate(layoutInflater, parent, false)
                createViewHolder(binding, binding.root)
            }
            ViewType.TOP.value -> {
                val binding = PreferenceBasicTopBinding.inflate(layoutInflater, parent, false)
                createViewHolder(binding, binding.root)
            }
            ViewType.MID.value -> {
                val binding = PreferenceBasicMidBinding.inflate(layoutInflater, parent, false)
                createViewHolder(binding, binding.root)
            }
            ViewType.DOWN.value -> {
                val binding = PreferenceBasicDownBinding.inflate(layoutInflater, parent, false)
                createViewHolder(binding, binding.root)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(hooks[position])
    }

    override fun getItemCount(): Int = hooks.size

    /**
     * 基础 ViewHolder
     */
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        protected abstract val titleView: android.widget.TextView
        protected abstract val summaryView: android.widget.TextView
        protected abstract val switchView: MaterialSwitch

        /**
         * 绑定 Hook 数据到视图
         * 关键：先移除 listener，设置状态，再添加 listener，避免 ViewHolder 复用时的动画问题
         */
        fun bind(hookItem: HookItem) {
            // 设置标题和描述
            titleView.text = hookItem.name
            if (hookItem.description.isNullOrBlank()) {
                summaryView.visibility = View.GONE
            } else {
                summaryView.visibility = View.VISIBLE
                summaryView.text = hookItem.description
            }

            // 关键修复：先移除 listener，避免 ViewHolder 复用时触发不必要的回调
            switchView.setOnCheckedChangeListener(null)
            
            // 设置开关状态
            val isEnabled = HookManager.isEnabled(hookItem.hook)
            switchView.isChecked = isEnabled
            
            // 重新设置开关监听
            switchView.setOnCheckedChangeListener { _, isChecked ->
                HookManager.setEnabled(hookItem.hook, isChecked)
            }
        }
    }

    /**
     * 统一的 ViewHolder 工厂方法
     * 消除重复代码，统一创建逻辑
     */
    private fun createViewHolder(binding: Any, root: View): BaseViewHolder {
        return object : BaseViewHolder(root) {
            override val titleView: android.widget.TextView = when (binding) {
                is PreferenceBasicSingleBinding -> binding.title
                is PreferenceBasicTopBinding -> binding.title
                is PreferenceBasicMidBinding -> binding.title
                is PreferenceBasicDownBinding -> binding.title
                else -> throw IllegalArgumentException("Unknown binding type")
            }
            
            override val summaryView: android.widget.TextView = when (binding) {
                is PreferenceBasicSingleBinding -> binding.summary
                is PreferenceBasicTopBinding -> binding.summary
                is PreferenceBasicMidBinding -> binding.summary
                is PreferenceBasicDownBinding -> binding.summary
                else -> throw IllegalArgumentException("Unknown binding type")
            }
            
            override val switchView: MaterialSwitch = root.findViewById(R.id.switchWidget)
        }
    }
}

