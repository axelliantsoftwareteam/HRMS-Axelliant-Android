package com.axelliant.android_erp.screens

import android.R.attr
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.axelliant.android_erp.R
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.config.AppConst.KEY_PARAM
import com.axelliant.android_erp.databinding.FragmentSplashBinding
import com.axelliant.android_erp.navigation.AppNavigator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSplashBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load GIF when running the app:
        loadGif()
    }

    private fun loadGif() {
        binding?.let {
            Glide.with(this)
                .asGif()  // Load as animated GIF
                .load(R.drawable.applogo)  // Call your GIF here (url, raw, etc.)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.setLoopCount(1)
                        resource?.registerAnimationCallback(object :
                            Animatable2Compat.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable) {
                                //do whatever after specified number of loops complete

                                AppNavigator.navigateToLogin()

                            }
                        })
                        return false
                    }

                })
                .into(it.myImageView)

        }
    }

}