package re.limus.timas.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import re.limus.timas.annotations.UiCategory
import re.limus.timas.databinding.FragmentHookListBinding
import re.limus.timas.hook.manager.HookManager
import re.limus.timas.ui.adapter.HookAdapter
import re.limus.timas.ui.base.BaseFragment

class HookListFragment : BaseFragment<FragmentHookListBinding>(FragmentHookListBinding::inflate) {

    private val category: UiCategory? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_CATEGORY, UiCategory::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_CATEGORY) as? UiCategory
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val filteredHooks = category?.let {
            HookManager.getAllHooks().filter { it.category == category }
        } ?: emptyList()

        binding.recyclerViewHooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = HookAdapter(filteredHooks)
        }
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        @JvmStatic
        fun newInstance(category: UiCategory) = HookListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_CATEGORY, category)
            }
        }
    }
}