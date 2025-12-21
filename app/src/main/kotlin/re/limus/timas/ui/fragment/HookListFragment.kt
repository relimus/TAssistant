package re.limus.timas.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import re.limus.timas.annotations.UiCategory
import re.limus.timas.databinding.FragmentHookListBinding
import re.limus.timas.hook.manager.HookManager
import re.limus.timas.ui.adapter.HookAdapter

class HookListFragment : Fragment() {

    private var _binding: FragmentHookListBinding? = null
    private val binding get() = _binding!!

    private var category: UiCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 对于 Android 13 (API 33) 及以上版本，使用新的 API
                it.getSerializable(ARG_CATEGORY, UiCategory::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_CATEGORY) as? UiCategory
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allHooks = HookManager.getAllHooks()
        val filteredHooks = if (category != null) {
            allHooks.filter { it.category == category }
        } else {
            emptyList()
        }

        binding.recyclerViewHooks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = HookAdapter(filteredHooks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        @JvmStatic
        fun newInstance(category: UiCategory) =
            HookListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORY, category)
                }
            }
    }
}
