package com.axelliant.hrms.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
class SessionManager ( val context: Context) {

    private var pref: SharedPreferences? = null

    // Editor for Shared preferences
    private var editor: SharedPreferences.Editor? = null

    // Context
    private var _context: Context? = null

    // Shared pref mode
    private val PRIVATE_MODE = 0

    // Sharedpref file name
    private val PREF_NAME = "UserPref"

    // All Shared Preferences Keys
    private val IS_LOGIN = "IsLoggedIn"
    private val IS_REMEMBER = "IsRememberMe"
    private val IS_Subscription = "isSubscription"
    private val isLastRemember = "isLastRemember"

    // email (make variable public to access from outside)
    private val KEY_EMAIL = "email"

    // Name (make variable public to access from outside)
    private val KEY_FNAME = "firstname"
    private val KEY_LNAME = "lastname"
    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_CNIC = "cnic"
    // User password (make variable public to access from outside)
    private val KEY_TOKEN = "token"
    private val KEY_EXPIRATION_TIME = "expiration_time"
    private val KEY_PROPERTYINFO = "property_info"
    private val KEY_PHASE = "phase"
    private val KEY_PAVILION = "pavilion"
    private val KEY_CLUSTER = "cluster"
    private val KEY_STREET = "street"


    private val Market_Permission = "marketPermission"
    private val Advertising_Permission = "advertisingPermission"
    private val Stric_PERMISSION = "stricPermission"


    private val Notificiation = "notificiation"

    init {
        _context = context
        pref = _context!!.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref!!.edit()
    }


    fun setMarketPermission(isAllowed:Boolean){
        editor!!.putBoolean(Market_Permission,isAllowed)
        editor!!.commit()
    }

    fun setAdvertisingPermission(isAllowed:Boolean){
        editor!!.putBoolean(Advertising_Permission,isAllowed)
        editor!!.commit()
    }

    fun setStrictlyPermission(isAllowed:Boolean){
        editor!!.putBoolean(Stric_PERMISSION,isAllowed)
        editor!!.commit()
    }

    fun setNotification(isAllowed:Boolean){
        editor!!.putBoolean(Notificiation,isAllowed)
        editor!!.commit()
    }


    fun getMarketPermission():Boolean{
        return pref!!.getBoolean(Market_Permission, false)
    }

    fun getAdvertisingPermission():Boolean{
        return pref!!.getBoolean(Advertising_Permission, false)
    }
    fun getStrictlyPermission():Boolean{
        return pref!!.getBoolean(Stric_PERMISSION, false)
    }


    fun getNotification():Boolean{
        return pref!!.getBoolean(Notificiation, false)
    }



    fun setRememberMe(checkValue:Boolean=false){
        editor!!.putBoolean(IS_REMEMBER,checkValue)
        editor!!.commit()
    }
    fun setSubscriptionSave(checkValue:Boolean=false){
        editor!!.putBoolean(IS_Subscription,checkValue)
        editor!!.commit()
    }

    fun getRememberMe():Boolean{
        return pref!!.getBoolean(IS_REMEMBER, false)
    }
    fun getSubscription():Boolean{
        return pref!!.getBoolean(IS_Subscription, false)
    }
    /**
     * Create login session  KEY_ADMINNAME
     */
    fun createLoginSession(username: String?,
                           userPass: String?,
                           accessToken: String?,
                           lastRemember:Boolean=false) {
        // Storing login value as TRUE
        editor!!.putBoolean(IS_LOGIN, true)
        editor!!.putBoolean(isLastRemember, lastRemember)
        // Storing password in pref
        // Storing name in pref
        editor!!.putString(KEY_USERNAME, username)
        editor!!.putString(KEY_PASSWORD, userPass)
        editor!!.putString(KEY_TOKEN, accessToken)
        //        editor.putString(KEY_TYPE, role);
        // commit changes
        editor!!.commit()

    }
    fun getRememberUserName():String{
        return if(isLoggedIn())
            pref!!.getString(KEY_USERNAME, "").toString()
        else
            ""

    }
    fun getRememberPassword():String{
        return if(isLoggedIn())
            pref!!.getString(KEY_PASSWORD, "").toString()
        else
            ""

    }


    /**
     * Check login method wil check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     */
//    fun checkLogin(): Boolean {
//        // Check login status
//        return if (isLoggedIn()) {
//            true
//        } else false
//    }
    fun checkLogin(): Boolean {
        // Check login status
        return isLoggedIn()
    }


    /**
     * Clear session details
     */
    fun logoutUser(): Boolean {
        editor!!.putBoolean(IS_LOGIN, false)
        editor!!.clear()
        editor!!.commit()
        return true
    }
    /**
     * Quick check for login
     */
    // Get Login State
    private fun isLoggedIn(): Boolean {
        return pref!!.getBoolean(IS_LOGIN, false)
    }

    fun saveCnic(name: String?) {
        editor!!.putString(KEY_CNIC, name)
        editor!!.commit()
    }
    fun saveFirstName(fname: String?) {
        editor!!.putString(KEY_FNAME, fname)
        editor!!.commit()
    }


    fun saveAddreess(lname: String?) {
        editor!!.putString(KEY_LNAME, lname)
        editor!!.commit()
    }


    fun saveToken(token: String?) {
        editor!!.putString(KEY_TOKEN, token)
        editor!!.commit()

    }


    fun savePassword(password: String?) {
        editor!!.putString(KEY_PASSWORD, password)
        editor!!.commit()
    }

    fun saveUserEmail(email: String?) {
        editor!!.putString(KEY_EMAIL, email)
        editor!!.commit()
    }
    fun saveUserPropertyinfo(propertyinfo: String?) {
        editor!!.putString(KEY_PROPERTYINFO, propertyinfo)
        editor!!.commit()
    }
    fun saveUserPavilion(pavilion: String?) {
        editor!!.putString(KEY_PAVILION, pavilion)
        editor!!.commit()
    }
    fun saveUserPhase(phase: String?) {
        editor!!.putString(KEY_PHASE, phase)
        editor!!.commit()
    }
    fun saveUserStreet(street: String?) {
        editor!!.putString(KEY_STREET, street)
        editor!!.commit()
    }
    fun saveUserCLuster(cluster: String?) {
        editor!!.putString(KEY_CLUSTER, cluster)
        editor!!.commit()
    }



    fun getUserEmail(): String? {
        return pref!!.getString(KEY_EMAIL, null)
    }

    fun getPropertyInfo(): String? {
        return pref!!.getString(KEY_PROPERTYINFO, null)
    }



    fun getPhase(): String? {
        return pref!!.getString(KEY_PHASE, null)
    }
    fun getStreet(): String? {
        return pref!!.getString(KEY_STREET, null)
    }
    fun getCluster(): String? {
        return pref!!.getString(KEY_CLUSTER, null)
    }

    fun getPavilion(): String? {
        return pref!!.getString(KEY_PAVILION, null)
    }



    fun getPassword(): String? {
        return pref!!.getString(KEY_PASSWORD, null)
    }




    fun getToken(): String? {
        return pref!!.getString(KEY_TOKEN, "")
    }

    fun isTokenExpired(): Boolean {
        val expirationTime = pref?.getLong(KEY_EXPIRATION_TIME, 0)
        return System.currentTimeMillis() >= expirationTime!!
    }

    fun getFirstName(): String? {
        return pref!!.getString(KEY_FNAME, null)
    }

    fun getAddress(): String? {
        return pref!!.getString(KEY_LNAME, null)
    }
    fun getCnic(): String? {
        return pref!!.getString(KEY_CNIC, null)
    }


}