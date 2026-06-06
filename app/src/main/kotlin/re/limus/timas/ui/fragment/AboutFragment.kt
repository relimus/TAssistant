package re.limus.timas.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import re.limus.timas.databinding.FragmentAboutBinding
import re.limus.timas.ui.base.BaseFragment

class AboutFragment : BaseFragment<FragmentAboutBinding>(FragmentAboutBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickAuthor()
    }

    private fun clickAuthor() {
        binding.button.setOnClickListener {
            val uri = "https://github.com/relimus".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AboutFragment()
    }
}
