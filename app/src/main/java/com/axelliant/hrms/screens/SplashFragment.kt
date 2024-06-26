package com.axelliant.hrms.screens

import android.R.attr
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.axelliant.hrms.R
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.config.AppConst.KEY_PARAM
import com.axelliant.hrms.databinding.FragmentSplashBinding
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.SessionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.App
import org.koin.android.ext.android.inject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class SplashFragment : BaseFragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding

    private val sessionManager: SessionManager by inject()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSplashBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load GIF when running the app:
        loadGif()
        getSignatureHash()
    }



    @RequiresApi(Build.VERSION_CODES.P)
    private fun getSignatureHash() {
        try {
            val info = requireContext().packageManager.getPackageInfo(
                "com.axelliant.android_erp",
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            for (signature in info.signingInfo.apkContentsSigners) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(
                    "KeyHash", "KeyHash:" + Base64.encodeToString(
                        md.digest(),
                        Base64.DEFAULT
                    )
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
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
                                if (sessionManager.checkLogin()) {
                                   AppNavigator.navigateToHome()
                                } else {
                                    AppNavigator.navigateToLogin()
                                }


                            }
                        })
                        return false
                    }

                })
                .into(it.myImageView)
        }
    }

}