package com.axelliant.hris.screens

import android.content.Context
import android.content.pm.PackageInfo
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
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentSplashBinding
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.SessionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load GIF when running the app:
        loadGif()
        getSignatureHash()
        printHashKey(requireContext())
    }

    fun printHashKey(pContext: Context) {
        try {
            val info: PackageInfo = pContext.getPackageManager()
                .getPackageInfo(pContext.getPackageName(), PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("KeyHash", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("KeyHash", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("KeyHash", "printHashKey()", e)
        }
    }

    private fun getSignatureHash() {
        try {
            val info = requireContext().packageManager.getPackageInfo(
                "com.axelliant.android_erp",
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            for (signature in if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners!!
            } else {
                TODO("VERSION.SDK_INT < P")
            }) {
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
        binding?.let { viewBinding ->
            Glide.with(this)
                .asGif()
                .load(R.drawable.applogo)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable,
                        model: Any,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource.setLoopCount(1)
                        resource.registerAnimationCallback(object :
                            Animatable2Compat.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable) {
                                AppNavigator.navigateToHome()
                            }
                        })
                        return false
                    }
                })
                .into(viewBinding.myImageView)
        }
    }
}
