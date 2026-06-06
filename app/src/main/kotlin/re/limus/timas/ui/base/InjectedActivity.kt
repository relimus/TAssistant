package re.limus.timas.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.viewbinding.ViewBinding
import top.sacz.xphelper.activity.BaseActivity

abstract class InjectedActivity <VB: ViewBinding>(
    private val bindingInflater: (layoutInflater: LayoutInflater) -> VB
) : BaseActivity() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = bindingInflater.invoke(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}