package com.axelliant.hris.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.AppDrawerAction
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.event.Event
import com.axelliant.hris.features.dashboard.data.UserPermissionRepository
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.repos.HomeRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepo,
    private val userPermissionRepository: UserPermissionRepository
) : BaseViewModel() {

     val dashboardResponse: MutableLiveData<Event<DashboardResponse?>> by lazy { MutableLiveData<Event<DashboardResponse?>>() }
     val employListResponse: MutableLiveData<Event<EmployProfileListModel?>> by lazy { MutableLiveData<Event<EmployProfileListModel?>>() }
     val todayTeamResponse: MutableLiveData<Event<TodayTeamResponse?>> by lazy { MutableLiveData<Event<TodayTeamResponse?>>() }
     val checkInResponse: MutableLiveData<Event<CheckInResponse?>> by lazy { MutableLiveData<Event<CheckInResponse?>>() }
     private val _drawerPermissionState =
         MutableStateFlow<UiState<Set<AppDrawerAction>>>(UiState.Idle)
     val drawerPermissionState: StateFlow<UiState<Set<AppDrawerAction>>> =
         _drawerPermissionState.asStateFlow()

    fun loadDrawerPermissions() {
        if (_drawerPermissionState.value is UiState.Loading) return

        viewModelScope.launch {
            _drawerPermissionState.value = UiState.Loading
            _drawerPermissionState.value = when (val result = userPermissionRepository.getAllowedDrawerActions()) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Success(emptySet())
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun getTodayTeamInfo() {
        isLoading.value = Event(true)
        homeRepo.getTeamAttendData()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    todayTeamResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    todayTeamResponse.value =null
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getDashboardInformation() {
        isLoading.value = Event(true)
        homeRepo.getDashboardData()
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    dashboardResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun postCheckIn(checkInRequest: CheckInRequest) {
        isLoading.value = Event(true)
        homeRepo.checkInAttendance(checkInRequest)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    checkInResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    checkInResponse.value = null
                    Log.d("Success VieModel->", "false")


                }
            }


    }

    fun getTodayTeamList(filter:String?) {
        isLoading.value = Event(true)
        homeRepo.getTodayEmployListData(filter)
            .observeForever { data ->
                // Handle the login response
                data?.let { baseModel ->
                    isLoading.value = Event(false)
                    // Handle success
                    employListResponse.value = Event(baseModel.message?.data)

                } ?: run {
                    isLoading.value = Event(false)
                    // Handle error
                    Log.d("Success VieModel->", "false")


                }
            }


    }



}
