package com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import chip.devicecontroller.ChipClusters.ElectricalEnergyMeasurementCluster
import chip.devicecontroller.ChipClusters.OperationalStateCluster
import chip.devicecontroller.ChipStructs
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.utils.DishWasherEnumConstants
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.repo.MatterDishWasherRepository
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.view.MatterDishwasherFragment.Companion.TIME_OUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DishwasherViewModel @Inject constructor(private val dishWasherRepository: MatterDishWasherRepository) :
    ViewModel() {


    // LiveData for energy measurement result
    private val _energyMeasurement =
        MutableLiveData<ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?>()
    val energyMeasurement: LiveData<ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?> =
        _energyMeasurement

    // LiveData for error
    private val _error = MutableLiveData<Exception?>()
    val error: LiveData<Exception?> = _error

    private val _energyMeasurementInResumeOrPauseState =
        MutableLiveData<ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?>()
    val energyMeasurementInResumeOrPauseState: LiveData<ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?> =
        _energyMeasurementInResumeOrPauseState

    // LiveData for error
    private val _errorInResumeOrPauseState = MutableLiveData<Exception?>()
    val errorInResumeOrPauseState: LiveData<Exception?> = _errorInResumeOrPauseState

    private val _deviceId = MutableLiveData<Long>()
    val deviceId: LiveData<Long> get() = _deviceId

    private val _pauseDishwasherSuccess = MutableLiveData<Boolean>()
    val pauseDishwasherSuccess: LiveData<Boolean> = _pauseDishwasherSuccess

    private val _pauseDishwasherError = MutableLiveData<Exception?>()
    val pauseDishwasherError: LiveData<Exception?> = _pauseDishwasherError

    private val _resumeDishwasherSuccess = MutableLiveData<Boolean>()
    val resumeDishwasherSuccess: LiveData<Boolean> = _resumeDishwasherSuccess

    private val _resumeDishwasherError = MutableLiveData<Exception?>()
    val resumeDishwasherError: LiveData<Exception?> = _resumeDishwasherError

    private val _stopDishwasherSuccess = MutableLiveData<Boolean>()
    val stopDishwasherSuccess: LiveData<Boolean> = _stopDishwasherSuccess

    private val _stopDishwasherError = MutableLiveData<Exception?>()
    val stopDishwasherError: LiveData<Exception?> = _stopDishwasherError

    private val _startDishwasherSuccess = MutableLiveData<Boolean>()
    val startDishwasherSuccess: LiveData<Boolean> = _startDishwasherSuccess

    private val _startDishwasherError = MutableLiveData<Exception?>()
    val startDishwasherError: LiveData<Exception?> = _startDishwasherError

    fun setDeviceId(id: Long) {
        _deviceId.value = id
    }

    fun getDeviceId(): Long? {
        return _deviceId.value
    }

    fun saveTotalEnergyConsumption(totalEnergyConsumed: Float) {
        viewModelScope.launch {
            dishWasherRepository.saveTotalEnergyConsumption(totalEnergyConsumed)
        }
    }

    fun getTotalEnergyConsumption(): LiveData<Float> {
        return liveData {
            emit(dishWasherRepository.getTotalEnergyConsumption())
        }
    }

    fun saveAverageEnergyPerCycle(averageEnergyPerCycleConsumed: Float) {
        viewModelScope.launch {
            dishWasherRepository.saveAverageEnergyPerCycle(averageEnergyPerCycleConsumed)
        }
    }

    fun getAverageEnergyPerCycle(): LiveData<Float> {
        return liveData {
            emit(dishWasherRepository.getAverageEnergyPerCycle())
        }
    }

    fun saveInCurrentCycleEnergyConsumed(inCurrentCycleEnergyConsumed: Float) {
        viewModelScope.launch {
            dishWasherRepository.saveInCurrentCycleEnergyConsumed(inCurrentCycleEnergyConsumed)
        }
    }

    fun getInCurrentCycleEnergyConsumed(): LiveData<Float> {
        return liveData {
            emit(dishWasherRepository.getInCurrentCycleEnergyConsumed())
        }
    }

    fun saveCompletedCycleCount(completedCycleCount: Int) {
        viewModelScope.launch {
            dishWasherRepository.saveCompletedCycleCount(completedCycleCount)
        }
    }

    fun getCompletedCycleCount(): LiveData<Int> {
        return liveData {
            emit(dishWasherRepository.getCompletedCycleCount())
        }
    }

    fun saveTimeLeftFormatted(timeLeftInMillSeconds: Long) {
        viewModelScope.launch {
            dishWasherRepository.saveTimeLeftFormatted(timeLeftInMillSeconds)
        }
    }

    fun getTimeLeftFormatted(): LiveData<Long> {
        return liveData {
            emit(dishWasherRepository.getTimeLeftFormatted())
        }
    }

    fun saveAppliedCycleStates(cycleStates: DishWasherEnumConstants) {
        viewModelScope.launch {
            dishWasherRepository.saveAppliedCycleStates(cycleStates)
        }
    }

    fun getAppliedCycleStates(): LiveData<DishWasherEnumConstants?> {
        return liveData {
            emit(dishWasherRepository.getAppliedCycleStates())
        }
    }

    fun saveCompletedCycleProgressBar(completedProgressBarCount: Int) {
        viewModelScope.launch {
            dishWasherRepository.saveCompletedCycleProgressBar(completedProgressBarCount)
        }
    }

    fun getCompletedCycleProgressBar(): LiveData<Int> {
        return liveData {
            emit(dishWasherRepository.getCompletedCycleProgressBar())
        }
    }


    fun readEnergy(context: Context?) {
        viewModelScope.launch {
            try {
                val deviceMag = dishWasherRepository.getEleDevMag(context!!, getDeviceId())
                deviceMag.subscribeCumulativeEnergyImportedAttribute(object :
                    ElectricalEnergyMeasurementCluster.CumulativeEnergyImportedAttributeCallback {
                    override fun onError(error: Exception?) {
                        _error.postValue(error)
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onSuccess(value: ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?) {
                        _energyMeasurement.postValue(value)
                        print("energyReadingRepo ${value?.energy}")
                    }
                }, 1, 10)
            } catch (exception: Exception) {
                _error.postValue(exception)
            }
        }
    }

    fun readEnergyWhenInPauseOrResumeState(context: Context?) {
        viewModelScope.launch {
            try {
                val deviceMag = dishWasherRepository.getEleDevMag(context!!, getDeviceId())
                deviceMag.subscribeCumulativeEnergyImportedAttribute(object :
                    ElectricalEnergyMeasurementCluster.CumulativeEnergyImportedAttributeCallback {
                    override fun onError(error: Exception?) {
                        _errorInResumeOrPauseState.postValue(error)
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onSuccess(value: ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?) {
                        _energyMeasurementInResumeOrPauseState.postValue(value)
                        print("energyReadingRepoResumeOrPauseState ${value?.energy}")
                    }
                }, 1, 2)
            } catch (exception: Exception) {
                _error.postValue(exception)
            }
        }
    }

    fun pauseDishwasher(context: Context, endPointId: Int) {
        viewModelScope.launch {
            dishWasherRepository.getDishwasherClusterForDevice(context, getDeviceId(), endPointId)
                .pause(object :
                    OperationalStateCluster.OperationalCommandResponseCallback {
                    override fun onError(error: java.lang.Exception?) {
                        _pauseDishwasherError.postValue(error)
                    }

                    override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                        _pauseDishwasherSuccess.postValue(true)
                    }
                }, TIME_OUT)
        }
    }

    fun resumeDishwasher(context: Context, endPointId: Int) {
        viewModelScope.launch {
            dishWasherRepository.getDishwasherClusterForDevice(context, getDeviceId(), endPointId)
                .resume(object :
                    OperationalStateCluster.OperationalCommandResponseCallback {
                    override fun onError(error: java.lang.Exception?) {
                        _resumeDishwasherError.postValue(error)
                    }

                    override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                        println("onResumeSuccess")
                        _resumeDishwasherSuccess.postValue(true)
                    }
                }, TIME_OUT)
        }

    }

    fun turnOffDishwasher(context: Context, endPointId: Int) {
        viewModelScope.launch {
            dishWasherRepository.getDishwasherClusterForDevice(context, getDeviceId(), endPointId)
                .stop(object :
                    OperationalStateCluster.OperationalCommandResponseCallback {
                    override fun onError(error: java.lang.Exception?) {
                        _stopDishwasherError.postValue(error)
                    }

                    override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                        _stopDishwasherSuccess.postValue(true)
                    }
                })
        }

    }

    fun turnOnDishwasher(context: Context, endPointId: Int) {
        viewModelScope.launch {
            dishWasherRepository.getDishwasherClusterForDevice(context,getDeviceId(),endPointId).start(object :
                OperationalStateCluster.OperationalCommandResponseCallback {
                override fun onError(error: java.lang.Exception?) {
                    _startDishwasherError.postValue(error)
                }

                override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                    _startDishwasherSuccess.postValue(true)
                }

            }, TIME_OUT)
        }

    }

}