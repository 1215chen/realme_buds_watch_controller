package com.cyhao.realmebuds.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 单 Activity 架构下的唯一 ViewModel：把 [BudsRepository] 的状态映射为 UI 状态，并转达用户操作。
 * 演示模式切换时会重建仓库实现。
 */
class BudsViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val settings = AppSettings(appContext)

    private var repository: BudsRepository =
        BudsRepositoryProvider.create(appContext, settings.demoMode)

    private val _state = MutableStateFlow(repository.state.value)
    val state: StateFlow<BudsState> = _state

    private val _scanning = MutableStateFlow(repository.scanning.value)
    val scanning: StateFlow<Boolean> = _scanning

    private val _scanResults = MutableStateFlow(repository.scanResults.value)
    val scanResults: StateFlow<List<ScannedDevice>> = _scanResults

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val messages: SharedFlow<Int> = _messages

    private val _demoMode = MutableStateFlow(settings.demoMode)
    val demoMode: StateFlow<Boolean> = _demoMode

    private val bindings = mutableListOf<Job>()

    init {
        bind(repository)
    }

    private fun bind(repo: BudsRepository) {
        bindings.forEach { it.cancel() }
        bindings.clear()
        bindings += viewModelScope.launch { repo.state.collect { _state.value = it } }
        bindings += viewModelScope.launch { repo.scanning.collect { _scanning.value = it } }
        bindings += viewModelScope.launch { repo.scanResults.collect { _scanResults.value = it } }
        bindings += viewModelScope.launch { repo.messages.collect { _messages.emit(it) } }
    }

    fun startScan() = repository.startScan()
    fun stopScan() = repository.stopScan()
    fun connect(device: ScannedDevice) = repository.connect(device)
    fun disconnect() = repository.disconnect()
    fun setAncMode(mode: AncMode) = repository.setAncMode(mode)
    fun setAncLevel(level: AncLevel) = repository.setAncLevel(level)
    fun setEq(preset: EqPreset) = repository.setEq(preset)
    fun setGesture(side: EarSide, type: GestureType, action: GestureAction) = repository.setGesture(side, type, action)
    fun setGameMode(enabled: Boolean) = repository.setGameMode(enabled)
    fun setDualConnection(enabled: Boolean) = repository.setDualConnection(enabled)
    fun setInEarDetection(enabled: Boolean) = repository.setInEarDetection(enabled)
    fun findBuds() = repository.findBuds()
    fun stopFindBuds() = repository.stopFindBuds()
    fun factoryReset() = repository.factoryReset()

    /** 切换演示模式：保存设置并重建仓库。 */
    fun setDemoMode(enabled: Boolean) {
        if (settings.demoMode == enabled) return
        val old = repository
        settings.demoMode = enabled
        _demoMode.value = enabled
        repository = BudsRepositoryProvider.create(appContext, enabled)
        old.close()
        bind(repository)
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer { BudsViewModel(appContext) }
            }
        }
    }
}
